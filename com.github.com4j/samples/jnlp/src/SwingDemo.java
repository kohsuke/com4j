
import com4j.ComException;
import wsh.ClassFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Swing-based demo that uses com4j.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class SwingDemo extends JFrame {
    private JTextField value;
    private JButton submit;
    private JTextField name;

    public SwingDemo() throws HeadlessException {
        super("com4j demo");

        Container panel = getContentPane();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        panel.add( new JLabel("Env var name:"), c );

        c = new GridBagConstraints();
        c.gridx=1;
        c.ipadx=200;
        c.fill=GridBagConstraints.BOTH;
        panel.add( name = new JTextField(), c );
        name.setEditable(true);

        c = new GridBagConstraints();
        c.gridx=2;
        panel.add( submit = new JButton("Enter"), c );

        c = new GridBagConstraints();
        c.gridy=1;
        c.gridwidth=3;
        c.fill=GridBagConstraints.BOTH;
        panel.add( value = new JTextField("Type in an environment variable (like 'PATH') and see its value"), c );
        value.setEditable(false);
        value.setBorder(new EmptyBorder(4,4,4,4));
        value.setBackground(getBackground());
        registerListeners();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        pack();
        setVisible(true);
        setResizable(false);
    }

    private void registerListeners() {
        ActionListener l = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                value.setText(expandEnvVar(name.getText()));
            }
        };
        submit.addActionListener(l);
        name.addActionListener(l);
    }


    public String expandEnvVar(String name) {
        try {
            name = '%'+name+'%';
            String value = ClassFactory.createWshShell().expandEnvironmentStrings(name);
            if(name.equals(value))
                return "Not a valid variable";
            else
                return value;
        } catch( ComException e ) {
            return e.getMessage();
        }
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        JFrame.setDefaultLookAndFeelDecorated(true);
        new SwingDemo();
    }
}
