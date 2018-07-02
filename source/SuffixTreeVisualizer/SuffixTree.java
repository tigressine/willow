// Part of Suffix Tree Visualizer by Tiger Sachse

package SuffixTreeVisualizer;

import javafx.scene.text.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.layout.*;

// Provides a suffix tree that can be built and printed in JavaFX.
public class SuffixTree {
    private Node root;
    private Color color;
    private int fontSize;
    private int diameter;
    private String string;
    private int ovalThickness;
    private int minimumLength;

    private final int SENTINEL = -1;
    private final int CHILDREN = 26;
    private final boolean DEBUG = false;

    // Constructor that builds the tree with some sensible defaults.
    public SuffixTree(String string) {
        diameter = 40;
        fontSize = 24;
        minimumLength = 100;
        ovalThickness = 10;
        color = Color.BLACK;
        
        this.string = string;
        
        build();
        
        if (DEBUG) {
            printDebuggingInformation();
        }
    }

    // Constructor builds the tree with custom visual parameters.
    public SuffixTree(String string,
                      Color color,
                      int diameter,
                      int fontSize,
                      int ovalThickness,
                      int minimumLength) {
        
        this.color = color;
        this.string = string;
        this.diameter = diameter;
        this.fontSize = fontSize;
        this.minimumLength = minimumLength;
        this.ovalThickness = ovalThickness;

        build();

        if (DEBUG) {
            printDebuggingInformation();
        }
    }

    // Build the suffix tree using Ukkonen's algorithm. This function is
    // certainly too long, but it is not easily split into subfunctions because
    // of the importance of the triple parameters from round to round. I apologize
    // in advance, but if you really want to know how this algorithm works I suggest
    // looking up some (much better) articles.
    private void build() {
        // POTENTIAL BUG: Dunno what happens if string is zero.
        root = new Node(0, 0, SENTINEL, CHILDREN);
       
        // Parameter triple to track location in the tree.
        int path = SENTINEL;
        int length = 0;
        int remaining = 0;

        Node current = root;

        if (DEBUG) {
            System.out.println("INDEX (NODE, EDGE, LENGTH) REMAINING");
        }

        // Process every character in the string.
        for (int stringIndex = 0; stringIndex < string.length(); stringIndex++) {
            Node previous = null;
            remaining++;
           
            // Get the integer value of the character at the current string index.
            int childIndex = string.charAt(stringIndex) - 'a';

            // Perform the algorithm as long as there are suffixes remaining.
            while (remaining > 0) {
                if (DEBUG) {
                    System.out.printf("%d (%s, %d, %d) %d\n",
                                      stringIndex,
                                      current.toString(),
                                      path,
                                      length,
                                      remaining);
                }

                // Check if insertion is at current node and not down an edge.
                if (path == SENTINEL || length == 0) {
                    // If the current node has no child for the given character,
                    // create that node and decrement remaining.
                    if (current.getChild(childIndex) == null) {
                        current.setChild(childIndex, new Node(stringIndex,
                                                              SENTINEL,
                                                              stringIndex,
                                                              CHILDREN));
                        remaining--;
                    }
                    // Else adjust the parameter triple and break the loop (rule one).
                    else {
                        path = childIndex;
                        length++;
                        break;
                        // POTENTIAL BUG: if this addition to length pushes into a
                        // child node then this would probably break the code. Very fixable
                        // and will be tested soon.
                    }
                }
                // Else check down an edge.
                else {
                    // Store the child for easy access.
                    Node child = current.getChild(path);
                   
                    // If the current character and the character on the edge
                    // at the appropriate length match, adjust the current
                    // parameters and break the loop.
                    if (string.charAt(child.getStart() + length) == string.charAt(stringIndex)) {
                        length++;
                        // Adjust the current node if the length extends past the
                        // present child's edge.
                        if (length >= child.getLength(stringIndex)) {
                            path = SENTINEL;
                            length = 0;
                            current = child;
                        }
                        break;
                    }
                    // Else the characters don't match and we need a new node.
                    else {
                        int compareIndex = string.charAt(child.getStart() + length) - 'a';

                        // Set the child's new stopping point, then create two new
                        // nodes. The first is for the rest of the child's old edge, and
                        // the second is for the mismatching character from the string.
                        child.setStop(child.getStart() + length);
                        child.setChild(compareIndex,
                                       new Node(child.getStop(),
                                                SENTINEL,
                                                child.getTerminus(),
                                                CHILDREN));
                        child.setChild(childIndex, new Node(stringIndex,
                                                            SENTINEL,
                                                            stringIndex,
                                                            CHILDREN));
                        
                        // If this is not the first newly created node, then apply rule two
                        // from Ukkonen's algorithm.
                        if (previous != null) {
                            if (DEBUG) {
                                System.out.printf("%s -> %s\n",
                                                  previous.toString(),
                                                  child.toString());
                            }

                            // Set the suffix link of the previous new node to point to
                            // the current new node.
                            previous.setLink(child);
                        }

                        // This node is now the previous node.
                        previous = child;
                       
                        // This child's terminus doesn't matter anymore, so it's set to
                        // a sentinel value.
                        child.setTerminus(SENTINEL);
                       
                        // If the current node is not the root, follow a suffix link
                        // if possible, (rule three).
                        if (current != root) {
                            if (current.getLink() != null) {
                                current = current.getLink();
                            }
                            else {
                                current = root;
                            }
                        }
                        // Else adjust the path and length values of the parameter triple.
                        else {
                            path = string.charAt(stringIndex - length + 1) - 'a';
                            length--;
                        }
                        
                        // One down! Several to go...
                        remaining--;
                    }
                }
            }
        }

        // After the conclusion of that nightmarish algorithm, replace any remaining
        // sentinels in the nodes.
        correctTerminalSentinels(root);
    }

    // Wrapper to draw the tree onto a context. This calls the recursive draw function
    // starting at the root of the tree, with an initial depth of diameter and
    // an inset of zero.
    public void draw(Pane pane, int width) {
        draw(pane, root, width, diameter, 0);
    }

    // Recursively draw each node and it's edges.
    private void draw(Pane pane, Node node, int width, int depth, int inset) {
        // What good is a recursive function without a base case?
        if (node == null) {
            return;
        }
        
        // A bunch of geometric variables used for drawing the ovals and edges.
        final int centerX = inset + (width / 2);
        // I just renamed depth here to match centerX...
        final int centerY = depth;
        final int ovalX = centerX - (diameter / 2);
        final int ovalY = centerY - (diameter / 2);
        final int innerDiameter = diameter - ovalThickness;
        final int innerOvalX = ovalX + (ovalThickness / 2);
        final int innerOvalY = ovalY + (ovalThickness / 2);

        int length = minimumLength;

        // Set the color of the drawing based on the instance variable for color.
        //context.setFill(color);
        //context.setStroke(color);

        // Count up all the children of this node.
        int children = 0;
        for (int childIndex = 0; childIndex < CHILDREN; childIndex++) {
            Node child = node.getChild(childIndex);
            if (child != null) {
                children++;
                
                // Set the length of the edges in this draw session to be the largest of all
                // the children's lengths, assuming any are larger than minimumLength.
                if (child.getLength() > length) {
                    length = child.getLength();
                }
            }
        }

        // Calculate how many horizontal slices are available based on the number of children.
        int widthSegment = width / ((children > 0) ? children : 1);

        // Set the target x coordinate for each edge line as i number of widthSegments,
        // with an added left-justified offset of inset. Also offset by half of a widthSegment.
        // The inset gives other nodes that haven't been visited yet to the left of the
        // current node room to print their children.
        for (int i = 0; i < children; i++) {
            int target = inset + (widthSegment / 2) + (i * widthSegment);
            StackPane linePane = new StackPane();
            Line line = new Line(centerX - target, 0, 0, length);
            
            //double degrees = Math.toDegrees(Math.atan2((centerX - target), length));

            int centerWidth = (target > centerX) ? centerX : target;
            int centerHeight = centerY;
            linePane.getChildren().add(line);
            //make this a function
            //
            String suffix = "hello";//string.substring(node.getStart(), node.getStop());
            Text suffixText = new Text(suffix);
            suffixText.setRotate(25);
            linePane.getChildren().add(suffixText);
            linePane.relocate(centerWidth, centerHeight);
            pane.getChildren().add(linePane);
            /*
            double slope = (centerX - target) / length;//div by zero
            double degrees = Math.toDegrees(Math.atan2((centerX - target), length));
            double lineLength = Math.sqrt(length * length + Math.pow(centerX - target, 2));
            //Line line = new Line(0, 0, lineLength, 0);
            line.setFill(color);
            linePane.getChildren().add(line);
            linePane.relocate(centerX - target, centerY + length);
            linePane.setRotate(degrees);
            pane.getChildren().add(linePane);
            //context.strokeLine(centerX, centerY, target, centerY + length); 
            */
        }

        // Draw two ovals, the outer oval being the color of the tree, and the
        // inner oval being white.
        StackPane ovalPane = new StackPane();
        System.out.printf("x%d y%d d%d\n", ovalX, ovalY, diameter/2);
        Circle outerOval = new Circle(ovalX, ovalY, diameter/2, color);//
        //Circle outerOval = new Circle(400, 25, 16, color);//
        Circle innerOval = new Circle(innerOvalX, innerOvalY, innerDiameter/2, Color.WHITE);
        ovalPane.getChildren().addAll(outerOval, innerOval);
        if (node.getTerminus() != SENTINEL) {
            Text text = new Text(Integer.toString(node.getTerminus()));
            ovalPane.getChildren().add(text);
        }
        ovalPane.relocate(ovalX, ovalY);
        pane.getChildren().add(ovalPane);//, innerOval);

        /* 
        context.fillOval(ovalX, ovalY, diameter, diameter);
        context.setFill(Color.WHITE);
        context.fillOval(innerOvalX, innerOvalY, innerDiameter, innerDiameter);
        context.setFill(color);
        */

        //************************************************ CONSTRUCTION AREA
        //context.setTextAlign(TextAlignment.CENTER);
        //context.setFont(new Font(fontSize));
        //context.fillText(Integer.toString(node.terminus), centerX, centerY);
        //context.fillText(string.substring(node.getStart(), node.getStop()), centerX, centerY);
        //*********************************************

        // Recursively call this function on all the children.
        for (int i = 0, j = 0; i < CHILDREN; i++) {
            if (node.getChild(i) != null) {
                int newInset = inset + widthSegment * j;
                draw(pane, node.getChild(i), widthSegment, depth + length, newInset);
                j++;
            }
        }
    }

    // During the build process a SENTINEL is used to indicate that the stop
    // index of an edge (inside a node) is running until the current childIndex of
    // the build algorithm. Once the algorithm is complete any remaining SENTINELs
    // must be set to equal the length of the full string, indicating that the stop
    // index of that edge is the end of the string. This function finds the SENTINELs
    // and replaces them appropriately.
    private void correctTerminalSentinels(Node node) {
        // Safety base case.
        if (node == null) {
            return;
        }
        
        // If this node contains a sentinel, it doesn't anymore!
        if (node.getStop() == SENTINEL) {
            node.setStop(string.length());
        }

        // Call this function on all children.
        for (int childIndex = 0; childIndex < CHILDREN; childIndex++) {
            correctTerminalSentinels(node.getChild(childIndex));
        }
    }

    // Public wrapper to call the recursive debugging information function,
    // starting at the root with an initial level of zero.
    public void printDebuggingInformation() {
        System.out.println("TERMINUS (START, STOP) SUFFIX");
        printDebuggingInformation(root, 0);
    }

    // Recursively print the tree to the terminal with debugging information.
    private void printDebuggingInformation(Node node, int level) {
        // Base case for the recursive function.
        if (node == null) {
            return;
        }
        
        // Print some fancy bars to indicate depth of the node.
        for (int braceCount = 0; braceCount < level; braceCount++) {
            System.out.printf("| ");
        }
   
        // Print the fundamental properties of this node.
        String suffix = string.substring(node.getStart(), node.getStop());
        System.out.printf("%d (%d %d) %s",
                          node.getTerminus(),
                          node.getStart(),
                          node.getStop(),
                          suffix);

        // Print a message if a suffix link exists.
        Node link = node.getLink();
        if (link != null) {
            System.out.printf(" | %s -> %s\n", node.toString(), link.toString());
        }
        else {
            System.out.printf("\n");
        }
        
        // Recursively call this function for all children of the node.
        for (int childIndex = 0; childIndex < CHILDREN; childIndex++) {
            printDebuggingInformation(node.getChild(childIndex), level + 1);
        }
    }
}

// Provides nodes for the suffix tree. This node also contains edge information.
// Storing the edge information with the nodes cuts down on the complexity and
// number of objects required in this implementation.
class Node {
    private int stop;
    private int start;
    private int length;
    private int terminus;
    private Node[] children;
    private Node suffixLink;

    // Construct a node with a provided terminus, as well as indices for the substring
    // contained on the edge leading into the node.
    public Node(int start, int stop, int terminus, int children) {
        length = 1;
        this.stop = stop;
        suffixLink = null;
        this.start = start;
        this.terminus = terminus;
        this.children = new Node[children];
    }

    // Getter for length. Only callable after build process is complete and
    // sentinels have been removed (e.g. once the constructor is finished).
    public int getLength() {
        return length;
    }

    // Getter for length. Can be called during the build process with a provided
    // index.
    public int getLength(int index) {
        if (stop > 0) {
            return length;
        }
        else {
            return index - start;
        }
    }

    // Getter for starting index of substring on incoming edge.
    public int getStart() {
        return start;
    }

    // Getter for ending index of substring on incoming edge.
    public int getStop() {
        return stop;
    }

    // Getter for terminating index of substring in string.
    public int getTerminus() {
        return terminus;
    }

    // Getter for child node of node.
    public Node getChild(int index) {
        return children[index];
    }

    // Getter for suffix link of node.
    public Node getLink() {
        return suffixLink;
    }
    
    // Set the ending index and update the length parameter.
    public void setStop(int stop) {
        this.stop = stop;
        length = stop - start;
    }

    // Setter for terminating index of substring in string.
    public void setTerminus(int terminus) {
        this.terminus = terminus;
    }

    // Setter for child at index in children array.
    public void setChild(int index, Node child) {
        children[index] = child;
    }

    // Setter for suffix link.
    public void setLink(Node suffixLink) {
        this.suffixLink = suffixLink;
    }
}
