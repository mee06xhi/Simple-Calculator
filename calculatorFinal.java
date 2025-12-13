import java.awt.*;  
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class calculatorFinal extends WindowAdapter implements ActionListener, KeyListener {
    
    // Frame dimensions
    private static final int FRAME_WIDTH = 360;
    private static final int FRAME_HEIGHT = 500;
    
    // Display dimensions
    private static final int DISPLAY_X = 50;
    private static final int DISPLAY_Y = 50;
    private static final int DISPLAY_WIDTH = 260;
    private static final int DISPLAY_HEIGHT = 60;
    private static final int MAX_DISPLAY_LENGTH = 20;
    
    // Button dimensions
    private static final int BUTTON_SIZE = 50;
    private static final int BUTTON_SPACING = 70;
    
    // Operator strings
    private static final String OPERATORS = "+-*/%";
    private static final String ERROR_PREFIX = "Error: ";
    private static final String EMPTY_STRING = "";
    
    // Pre-compiled regex patterns for better performance
    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?");
    private static final Pattern LAST_NUMBER_PATTERN = Pattern.compile("([+\\-*/%]|^)(-?\\d*\\.?\\d*)$");    

    // Operator precedence mapping
    private static final Map<String, Integer> PRECEDENCE = new HashMap<>();
    static {
        PRECEDENCE.put("+", 1);
        PRECEDENCE.put("-", 1);
        PRECEDENCE.put("*", 2);
        PRECEDENCE.put("/", 2);
        PRECEDENCE.put("%", 2);
    }

    // Color scheme
    private static final Color DISPLAY_BG = new Color(30, 30, 30);
    private static final Color NUMBER_BTN_BG = new Color(70, 70, 70);    
    private static final Color OPERATOR_BTN_BG = new Color(160, 140, 200);
    private static final Color UTILITY_BTN_BG = new Color(100, 100, 100); 
    private static final Color EQUALS_BTN_BG = new Color(190, 170, 230);  
    
    //Represents the current state of the calculator
    private enum State {
        EMPTY,           // No input yet
        ENTERING_NUMBER, // Currently entering a number
        AFTER_OPERATOR,  // Just entered an operator
        AFTER_RESULT,    // Just displayed a result
        ERROR            // Error state
    }
    
    //Enum for different error types with user-friendly messages
    private enum CalculatorError {
        DIVISION_BY_ZERO("Divide by 0"),
        MODULO_BY_ZERO("Modulo by 0"),
        INVALID_EXPRESSION("Invalid Input"),
        MATH_ERROR("Math Error"),
        OVERFLOW("Number too large");
        
        private final String message;
        
        CalculatorError(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return ERROR_PREFIX + message;
        }
    }
        
    // UI Components
    private final Frame frame;
    private final Label display;
    private final Map<String, Button> buttons;
    
    // Calculator state
    private State currentState;
    
    
    //Initializes the calculator application. Creates the frame, display, and all buttons
    public calculatorFinal() {
        frame = new Frame("Scientific Calculator");
        frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);  
        frame.setLayout(null);
        frame.setResizable(false);
        frame.setBackground(new Color(20, 20, 20)); // Deep black
        
        // Initialize state
        currentState = State.EMPTY;
        
        // Create and add display
        display = createDisplay();
        frame.add(display);
        
        // Create all buttons
        buttons = new HashMap<>();
        createAllButtons();
        
        // Add listeners
        frame.addWindowListener(this);
        frame.addKeyListener(this);
        frame.setFocusable(true);
        
        // Center frame on screen
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    private Label createDisplay() {
        Label label = new Label(EMPTY_STRING);
        label.setBackground(DISPLAY_BG);
        label.setForeground(new Color(220, 220, 255));
        label.setBounds(DISPLAY_X, DISPLAY_Y, DISPLAY_WIDTH, DISPLAY_HEIGHT);
        label.setAlignment(Label.RIGHT);
        label.setFont(new Font("Monospaced", Font.BOLD, 24));
        return label;
    }
    private Button createButton(String text, int x, int y, int w, int h, Color bgColor) {
        Button button = new Button(text);
        button.setBounds(x, y, w, h);
        button.setActionCommand(text);
        button.addActionListener(this);
        button.setFont(new Font("Arial", Font.BOLD, 18));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusable(false);
        frame.add(button);
        buttons.put(text, button);
        return button;
    }
    
    //Creates all calculator buttons (numbers, operators, utilities)
    private void createAllButtons() {
        createNumberButtons();
        createOperatorButtons();
        createUtilityButtons();
    }
    
    //Creates number buttons (0-9) in calculator layout
    private void createNumberButtons() {
        // Button positions: {x, y} for each number 0-9
        int[][] positions = {{120, 410}, {50, 340}, {120, 340}, {190, 340}, {50, 270}, {120, 270}, {190, 270}, {50, 200}, {120, 200},
                            {190, 200}};
        
        for (int i = 0; i <= 9; i++) {
            createButton(
                String.valueOf(i),
                positions[i][0],
                positions[i][1],
                BUTTON_SIZE,
                BUTTON_SIZE,
                NUMBER_BTN_BG
            );
        }
    }
    
    //Creates operator buttons (+, -, *, /, %, =)
    private void createOperatorButtons() {
        createButton("+", 260, 340, BUTTON_SIZE, BUTTON_SIZE, OPERATOR_BTN_BG);
        createButton("-", 260, 270, BUTTON_SIZE, BUTTON_SIZE, OPERATOR_BTN_BG);
        createButton("*", 260, 200, BUTTON_SIZE, BUTTON_SIZE, OPERATOR_BTN_BG);
        createButton("/", 260, 130, BUTTON_SIZE, BUTTON_SIZE, OPERATOR_BTN_BG);
        createButton("%", 190, 130, BUTTON_SIZE, BUTTON_SIZE, OPERATOR_BTN_BG);
        createButton("=", 260, 410, BUTTON_SIZE, BUTTON_SIZE, EQUALS_BTN_BG);
    }
    
    //Creates utility buttons (CE, back, +/-, .)
    private void createUtilityButtons() {
        createButton("CE", 50, 130, BUTTON_SIZE, BUTTON_SIZE, UTILITY_BTN_BG);
        createButton("back", 120, 130, BUTTON_SIZE, BUTTON_SIZE, UTILITY_BTN_BG);
        createButton("+/-", 50, 410, BUTTON_SIZE, BUTTON_SIZE, UTILITY_BTN_BG);
        createButton(".", 190, 410, BUTTON_SIZE, BUTTON_SIZE, UTILITY_BTN_BG);
    }
   
    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        
        // Clear error state when starting new input (except for CE and back)
        if (currentState == State.ERROR && !command.equals("CE") && !command.equals("back")) {
            display.setText(EMPTY_STRING);
            currentState = State.EMPTY;
        }
        
        // Clear display after result if entering new number
        if (currentState == State.AFTER_RESULT && isDigit(command)) {
            display.setText(EMPTY_STRING);
            currentState = State.EMPTY;
        }
        
        // Route to appropriate handler
        if (isDigit(command)) {
            handleNumberInput(command);
        } else if (isOperator(command)) {
            handleOperatorInput(command);
        } else {
            handleUtilityInput(command);
        }
    }
    
    //Handles number button clicks (0-9)
    private void handleNumberInput(String number) {
        String current = display.getText();
        
        // Prevent display overflow
        if (current.length() >= MAX_DISPLAY_LENGTH) {
            return;
        }
        
        updateDisplayLabel(number);
        currentState = State.ENTERING_NUMBER;
    }
    
    //Handles operator button clicks (+, -, *, /, %)
    private void handleOperatorInput(String operator) {
        String current = display.getText();
        
        // Don't allow operators if display is empty or ends with operator
        if (current.isEmpty() || endsWithOperator(current)) {
            return;
        }
        
        updateDisplayLabel(operator);
        currentState = State.AFTER_OPERATOR;
    }
    
    //Handles utility button clicks (CE, back, +/-, ., =)
    private void handleUtilityInput(String command) {
        switch (command) {
            case "CE":
                handleClear();
                break;
            case "back":
                handleBackspace();
                break;
            case "+/-":
                handleNegate();
                break;
            case ".":
                handleDecimal();
                break;
            case "=":
                handleEquals();
                break;
        }
    }
    
    //Clears the display and resets calculator state
    private void handleClear() {
        display.setText(EMPTY_STRING);
        currentState = State.EMPTY;
    }
    
    //Removes the last character from display (backspace)
    private void handleBackspace() {
        String current = display.getText();
        if (!current.isEmpty()) {
            display.setText(current.substring(0, current.length() - 1));
            
            // Update state based on remaining content
            if (display.getText().isEmpty()) {
                currentState = State.EMPTY;
            }
        }
    }
    
    //Toggles the sign of the last number in the expression
    private void handleNegate() {
        String current = display.getText();
        
        // If empty, add minus sign
        if (current.isEmpty()) {
            display.setText("-");
            currentState = State.ENTERING_NUMBER;
            return;
        }
        
        // Get the last number in the expression
        String lastNum = getLastNumber(current);
        
        // Can't negate if no number or just a minus sign
        if (lastNum.isEmpty() || lastNum.equals("-")) {
            return;
        }
        
        // Toggle negative sign
        String newNum;
        if (lastNum.startsWith("-")) {
            newNum = lastNum.substring(1); // Remove negative
        } else {
            newNum = "-" + lastNum; // Add negative
        }
        
        display.setText(replaceLastNumber(current, newNum));
    }
    
    //Adds a decimal point to the current number. Validates that current number doesn't already have a decimal
    private void handleDecimal() {
        String current = display.getText();
        
        // Only add decimal if current number doesn't already have one
        if (!canAddDecimal(current)) {
            return;
        }
        
        // If empty or ends with operator, add "0."
        if (current.isEmpty() || endsWithOperator(current)) {
            updateDisplayLabel("0.");
        } else {
            updateDisplayLabel(".");
        }
        
        currentState = State.ENTERING_NUMBER;
    }
    
    //Evaluates the expression and displays the result
    private void handleEquals() {
        String exp = display.getText();
        
        // Validate expression is not empty and doesn't end with operator
        if (exp.isEmpty() || endsWithOperator(exp)) {
            showError(CalculatorError.INVALID_EXPRESSION);
            return;
        }
        
        try {
            // Evaluate the expression
            double result = evaluateExpression(exp);
            
            // Check for infinity or NaN (invalid math operations)
            if (Double.isInfinite(result) || Double.isNaN(result)) {
                showError(CalculatorError.MATH_ERROR);
            } else {
                // Format and display result
                String formattedResult = formatResult(result);
                
                // Check if result fits in display
                if (formattedResult.length() > MAX_DISPLAY_LENGTH) {
                    showError(CalculatorError.OVERFLOW);
                } else {
                    display.setText(formattedResult);
                    currentState = State.AFTER_RESULT;
                }
            }
        } catch (ArithmeticException ex) {
            // Handle division/modulo by zero
            if (ex.getMessage().contains("Division")) {
                showError(CalculatorError.DIVISION_BY_ZERO);
            } else {
                showError(CalculatorError.MODULO_BY_ZERO);
            }
        } catch (Exception ex) {
            // Handle any other errors
            showError(CalculatorError.INVALID_EXPRESSION);
        }
    }
    
    //Handles keyboard input for calculator operations
    @Override
    public void keyPressed(KeyEvent e) {
        char key = e.getKeyChar();
        int keyCode = e.getKeyCode();
        
        // Number keys
        if (key >= '0' && key <= '9') {
            handleNumberInput(String.valueOf(key));
        }
        // Operator keys
        else if (key == '+' || key == '-' || key == '*' || key == '/' || key == '%') {
            handleOperatorInput(String.valueOf(key));
        }
        // Decimal point
        else if (key == '.') {
            handleDecimal();
        }
        // Equals (Enter or =)
        else if (key == '=' || keyCode == KeyEvent.VK_ENTER) {
            handleEquals();
        }
        // Backspace
        else if (keyCode == KeyEvent.VK_BACK_SPACE) {
            handleBackspace();
        }
        // Clear (Escape or Delete)
        else if (keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_DELETE) {
            handleClear();
        }
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
        // Not used, but required by KeyListener interface
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        // Not used, but required by KeyListener interface
    }
    
    //Checks if a string represents a single digit (0-9)
    private boolean isDigit(String str) {
        return str.length() == 1 && Character.isDigit(str.charAt(0));
    }
    
    //Checks if a string is a valid operator
    private boolean isOperator(String str) {
        return str.length() == 1 && OPERATORS.contains(str);
    }
    
    //Checks if the expression ends with an operator
    private boolean endsWithOperator(String exp) {
        if (exp.isEmpty()) return false;
        char last = exp.charAt(exp.length() - 1);
        return OPERATORS.indexOf(last) >= 0;
    }
    
    //Checks if a decimal point can be added to the current number
    private boolean canAddDecimal(String exp) {
        String lastNum = getLastNumber(exp);
        return !lastNum.contains(".");
    }
    
    //Extracts the last number from an expression
    private String getLastNumber(String exp) {
        if (exp.isEmpty()) return EMPTY_STRING;
        
        Matcher matcher = LAST_NUMBER_PATTERN.matcher(exp);
        
        if (matcher.find()) {
            return matcher.group(2);
        }
        return EMPTY_STRING;
    }
    
    //Replaces the last number in an expression with a new number
    private String replaceLastNumber(String exp, String newNum) {
        if (exp.isEmpty()) return newNum;
        
        Matcher matcher = LAST_NUMBER_PATTERN.matcher(exp);
        
        if (matcher.find()) {
            return exp.substring(0, matcher.start(2)) + newNum;
        }
        return exp;
    }
    
    //Formats a double result for display. Removes unnecessary .0 from integers
    private String formatResult(double result) {
        // Check if result is effectively an integer
        if (result == (long)result) {
            return String.valueOf((long)result);
        }
        
        // Format with reasonable precision
        String formatted = String.format("%.10f", result);
        
        // Remove trailing zeros after decimal point
        formatted = formatted.replaceAll("0+$", "");
        
        // Remove decimal point if nothing after it
        formatted = formatted.replaceAll("\\.$", "");
        
        return formatted;
    }
    
    //Updates the display label by appending new value
    private void updateDisplayLabel(String newValue) {
        String current = display.getText();
        display.setText(current + newValue);
    }
    
    //Displays an error message on the calculator
    private void showError(CalculatorError error) {
        display.setText(error.getMessage());
        currentState = State.ERROR;
    }
    
    //Evaluates a mathematical expression using the Shunting Yard algorithm. Converts infix notation to postfix, then evaluates
    private double evaluateExpression(String exp) {
        List<String> tokens = tokenize(exp);
        List<String> postfix = convertToPostfix(tokens);
        return evaluatePostfix(postfix);
    }
    
    //Tokenizes an expression into numbers and operators
    private List<String> tokenize(String exp) {
        List<String> tokens = new ArrayList<>();
        int i = 0;
        
        while (i < exp.length()) {
            char ch = exp.charAt(i);
            
            // Check if it's a digit or decimal point (start of a number)
            if (Character.isDigit(ch) || ch == '.') {
                StringBuilder num = new StringBuilder();
                
                // Read the entire number (including decimal)
                while (i < exp.length() && (Character.isDigit(exp.charAt(i)) || exp.charAt(i) == '.')) {
                    num.append(exp.charAt(i));
                    i++;
                }
                tokens.add(num.toString());
            }
            // Check if it's a minus sign that represents a negative number
            // (at start of expression or after an operator)
            else if (ch == '-' && (tokens.isEmpty() || isOperator(tokens.get(tokens.size() - 1)))) {
                StringBuilder num = new StringBuilder("-");
                i++; // Move past the minus sign
                
                // Read the number after the minus sign
                while (i < exp.length() && (Character.isDigit(exp.charAt(i)) || exp.charAt(i) == '.')) {
                    num.append(exp.charAt(i));
                    i++;
                }
                
                // Only add if we got a complete negative number
                if (num.length() > 1) {
                    tokens.add(num.toString());
                } else {
                    // Just a minus sign, treat as operator
                    tokens.add("-");
                }
            }
            // It's an operator or parenthesis
            else if (OPERATORS.indexOf(ch) >= 0 || ch == '(' || ch == ')') {
                tokens.add(String.valueOf(ch));
                i++;
            }
            // Skip any other characters (spaces, etc.)
            else {
                i++;
            }
        }
        
        return tokens;
    }
    
    //Converts infix notation to postfix notation using Shunting Yard algorithm
    private List<String> convertToPostfix(List<String> tokens) {
        List<String> output = new ArrayList<>();
        Stack<String> opStack = new Stack<>();
        
        for (String token : tokens) {
            // If token is a number, add to output
            if (NUMBER_PATTERN.matcher(token).matches()) {
                output.add(token);
            }
            // If token is an operator
            else if (PRECEDENCE.containsKey(token)) {
                // Pop operators with higher or equal precedence
                while (!opStack.isEmpty() && 
                       PRECEDENCE.containsKey(opStack.peek()) && 
                       PRECEDENCE.get(opStack.peek()) >= PRECEDENCE.get(token)) {
                    output.add(opStack.pop());
                }
                opStack.push(token);
            }
            // If token is opening parenthesis
            else if (token.equals("(")) {
                opStack.push(token);
            }
            // If token is closing parenthesis
            else if (token.equals(")")) {
                // Pop until matching opening parenthesis
                while (!opStack.isEmpty() && !opStack.peek().equals("(")) {
                    output.add(opStack.pop());
                }
                if (!opStack.isEmpty() && opStack.peek().equals("(")) {
                    opStack.pop(); // Remove the opening parenthesis
                }
            }
        }
        
        // Pop remaining operators
        while (!opStack.isEmpty()) {
            output.add(opStack.pop());
        }
        
        return output;
    }
    
    //Evaluates a postfix expression
    private double evaluatePostfix(List<String> postfix) {
        Stack<Double> stack = new Stack<>();
        
        for (String token : postfix) {
            // If token is a number, push to stack
            if (NUMBER_PATTERN.matcher(token).matches()) {
                stack.push(Double.parseDouble(token));
            }
            // If token is an operator, pop two operands and apply operation
            else {
                double b = stack.pop();
                double a = stack.pop();
                
                switch (token) {
                    case "+":
                        stack.push(a + b);
                        break;
                    case "-":
                        stack.push(a - b);
                        break;
                    case "*":
                        stack.push(a * b);
                        break;
                    case "/":
                        if (b == 0) {
                            throw new ArithmeticException("Division by zero");
                        }
                        stack.push(a / b);
                        break;
                    case "%":
                        if (b == 0) {
                            throw new ArithmeticException("Modulo by zero");
                        }
                        stack.push(a % b);
                        break;
                }
            }
        }
        
        return stack.pop();
    }
    
    //Handles window closing event. Properly disposes of resources and exits application
    @Override
    public void windowClosing(WindowEvent e) {
        frame.dispose();
        System.exit(0);
    }
    
    public static void main(String[] args) {
        // Set system look and feel for better appearance
        try {
            System.setProperty("awt.useSystemAAFontSettings", "on");
        } catch (Exception e) {
            // Ignore if property cannot be set
        }
        
        // Create calculator on Event Dispatch Thread
        EventQueue.invokeLater(() -> new calculatorFinal());
    }
}