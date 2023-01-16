import com.intellij.ui.JBColor;

import javax.swing.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.event.*;

public class ConvertToDataClassDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonCancel;
    private JProgressBar progressBar1;
    private final DataClassConversionHelper dataClassConversionHelper;
    static int value = 0;

    public ConvertToDataClassDialog(DataClassConversionHelper dataClassConversionHelper) {
        this.dataClassConversionHelper = dataClassConversionHelper;
        setContentPane(contentPane);
        setModal(true);
        init();

    }

    private void init() {
        getRootPane().setDefaultButton(buttonCancel);
        progressBar1.setUI(new BasicProgressBarUI() {
            protected Color getSelectionBackground() {
                return JBColor.ORANGE;
            }

            protected Color getSelectionForeground() {
                return JBColor.WHITE;
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        progressBar1.setValue(value);
        progressBar1.setVisible(true);
    }

//    private void onOK() {
//        // add your code here
//        value += 5;
//        progressBar1.setValue(value);
//        //dispose();
//    }

    private void onCancel() {
        // add your code here if necessary
        //dataClassConversionHelper.cancelConversion();
        dispose();
    }

    void setProgressValue(int progress) {
        progressBar1.setValue(progress);
        if (progress == 100) {
            dispose();
        }
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
