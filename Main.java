import javax.swing.*;

public class Main {


    public static void main(String[] args) throws Exception {
        System.out.println("Hello World");

        App window = new App();
        window.setSize(500, 500);
        window.setVisible(true);
        window.setTitle("Splotify");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}