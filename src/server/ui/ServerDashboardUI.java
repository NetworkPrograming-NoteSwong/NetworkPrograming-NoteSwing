package server.ui;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ServerDashboardUI {
    private JFrame frame;
    private JTextArea t_display;
    private JButton b_exit;
    private JButton b_connect;
    private JButton b_disconnect;

    private Runnable onStartServer;     // "서버 시작" 눌렀을 때 호출
    private Runnable onStopServer;      // "서버 종료" 눌렀을 때 호출

    public ServerDashboardUI() {
        frame = new JFrame("NoteSwing Server");
        frame.setLayout(new BorderLayout());
        frame.setSize(500, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

       buildGUI();

        frame.setVisible(true);
    }

    public void buildGUI() {
        frame.add(createDisplayPanel(), BorderLayout.CENTER);
        frame.add(createControlPanel(), BorderLayout.SOUTH);
    }

    public JPanel createDisplayPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        t_display = new JTextArea();
        t_display.setEditable(false);
        t_display.setBackground(Color.WHITE);

        panel.add(t_display);

        return panel;
    }

    public JPanel createControlPanel() {
        JPanel panel = new JPanel(new GridLayout(0,3));

        b_connect = new JButton("서버 시작");
        b_disconnect = new JButton("서버 종료");
        b_exit = new JButton("종료하기");

        b_disconnect.setEnabled(false);


        b_connect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                b_disconnect.setEnabled(true);
                b_connect.setEnabled(false);
                b_exit.setEnabled(false);

                if (onStartServer != null) {
                    onStartServer.run();
                }
            }
        });

        b_disconnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                b_disconnect.setEnabled(false);
                b_connect.setEnabled(true);
                b_exit.setEnabled(true);

                if (onStopServer != null) {
                    onStopServer.run();
                }
            }
        });
        b_exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
            }
        });

        panel.add(b_connect);
        panel.add(b_disconnect);
        panel.add(b_exit);

        return panel;
    }

    public void printDisplay(String text) {
        t_display.append(text + "\n");
        t_display.setCaretPosition(t_display.getDocument().getLength());
    }

    public void setOnStartServer(Runnable r) {
        this.onStartServer = r;
    }

    public void setOnStopServer(Runnable r) {
        this.onStopServer = r;
    }
}

