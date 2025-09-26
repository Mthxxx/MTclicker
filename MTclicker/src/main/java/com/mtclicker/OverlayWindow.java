package com.mtclicker;

import javax.swing.*;
import java.awt.*;

public class OverlayWindow extends JWindow {

    private JLabel powerLabel;
    private JLabel cpsLabel;
    private JLabel toggleLabel;
    private Image backgroundImage;
    private Font minhaFonte;

    public OverlayWindow() {
        // Carrega a imagem de fundo
        backgroundImage = new ImageIcon(getClass().getResource("/janelas/fundo_overlay.png")).getImage();

        // Carrega a fonte igual do MTclicker
        try {
            minhaFonte = Font.createFont(Font.TRUETYPE_FONT,
                    getClass().getResourceAsStream("/fontes/Coiny-Regular.ttf"));
            minhaFonte = minhaFonte.deriveFont(Font.BOLD, 20f);
        } catch (Exception e) {
            e.printStackTrace();
            minhaFonte = new Font("Arial", Font.BOLD, 20);
        }

        // Painel principal com fundo semi-transparente
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f)); // 50% transparente
                    g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                    g2d.dispose();
                }
            }
        };
        panel.setLayout(null);
        panel.setOpaque(false);
        setContentPane(panel);

        // Tamanho da janela
        int width = 150;
        int height = 70;
        setSize(width, height);

        // Labels do overlay
        powerLabel = new JLabel("POWER: OFF");
        powerLabel.setFont(minhaFonte.deriveFont(Font.BOLD, 12f));
        powerLabel.setForeground(Color.white);
        powerLabel.setBounds(10, 5, 200, 20);
        panel.add(powerLabel);

        cpsLabel = new JLabel("CPS: 0");
        cpsLabel.setFont(minhaFonte.deriveFont(Font.BOLD, 12f));
        cpsLabel.setForeground(Color.white);
        cpsLabel.setBounds(10, 25, 200, 20);
        panel.add(cpsLabel);

        toggleLabel = new JLabel("TOGGLE: None");
        toggleLabel.setFont(minhaFonte.deriveFont(Font.BOLD, 12f));
        toggleLabel.setForeground(Color.white);
        toggleLabel.setBounds(10, 45, 200, 20);
        panel.add(toggleLabel);

        // Sempre no topo
        setAlwaysOnTop(true);

        // Torna a janela completamente transparente e click-through
        setBackground(new Color(0, 0, 0, 0));
        setFocusableWindowState(false);

        // Hack para click-through: ignora todos os eventos do mouse
        addMouseListener(new java.awt.event.MouseAdapter() {});
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {});
    }

    // Atualiza os dados do overlay
    public void updateOverlay(boolean powerOn, int cps, String toggleKey) {
        powerLabel.setText("POWER: " + (powerOn ? "ON" : "OFF"));
        cpsLabel.setText("CPS: " + cps);
        toggleLabel.setText("TOGGLE: " + toggleKey);
        repaint();
    }
}
