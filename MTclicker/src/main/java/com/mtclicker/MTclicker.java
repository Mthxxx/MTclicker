package com.mtclicker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseListener;

import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class MTclicker extends JFrame {

    private Image backgroundImage;
    private Point mouseClickPoint;
    private JLabel powerButton;

    private boolean isOn = false; // estado visual do botão
    private ArrayList<ImageIcon> onFrames = new ArrayList<>();
    private ArrayList<ImageIcon> offFrames = new ArrayList<>();
    private Timer animationTimer;
    private int currentFrame = 0;

    // Outros botões
    private JLabel overlayButton, rightClickButton, toggleButton, scrollButton;
    private RotatingLabel scrollMark;
    private JLabel scrollBar;

    private boolean overlayOn = true;
    private boolean rightClickOn = false;
    private String toggleKeyName = "F7"; // nome da tecla selecionada
    private JLabel toggleKeyLabel;

    // Janela overlay
    private OverlayWindow overlayWindow;

    private int scrollValue = 25; // CPS inicial (0-50)

    // Contador global de clicks
    private JLabel cpsCounterLabel;
    private int globalClickCount = 0;

    private Timer clickTimer;
    private Robot robot;
    private boolean clickerOn = false; // estado real do autoclicker

    public MTclicker() {
        setTitle("MTclicker");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true);
        setBackground(new Color(0,0,0,0));

        // --- Ícone do aplicativo ---
        URL iconUrl = MTclicker.class.getResource("/icon.png");
        if (iconUrl != null) {
            ImageIcon icon = new ImageIcon(iconUrl);
            setIconImage(icon.getImage());
        } else System.err.println("⚠ Ícone principal não encontrado!");

        try { robot = new Robot(); }
        catch (AWTException ex) { ex.printStackTrace(); }

        // Carrega imagem de fundo
        backgroundImage = loadImage("/fundo/fundo_console.png");

        int width = (int)(backgroundImage.getWidth(null)*1.5);
        int height = (int)(backgroundImage.getHeight(null)*1.5);
        setSize(width,height);

        JPanel panel = new JPanel(){
            @Override
            protected void paintComponent(Graphics g){
                super.paintComponent(g);
                g.drawImage(backgroundImage,0,0,getWidth(),getHeight(),this);
            }
        };
        panel.setOpaque(false);
        panel.setLayout(null);
        setContentPane(panel);

        // Carrega frames ON e OFF para powerButton
        for(int i=1;i<=13;i++){
            onFrames.add(scaleIcon(loadIcon("/botoes/botao_on/botao_on"+i+".png")));
            offFrames.add(scaleIcon(loadIcon("/botoes/botao_off/botao_off"+i+".png")));
        }

        // Power Button inicia OFF13
        powerButton = new JLabel(offFrames.get(12));
        powerButton.setBounds(30,240, offFrames.get(12).getIconWidth(), offFrames.get(12).getIconHeight());
        panel.add(powerButton);

        powerButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                togglePower();
            }
        });

        // Overlay Button
        overlayButton = new JLabel(scaleIcon(loadIcon("/botoes/botoes/botao_overlay_on.png")));
        overlayButton.setBounds(33,170,overlayButton.getIcon().getIconWidth(), overlayButton.getIcon().getIconHeight());
        panel.add(overlayButton);
        overlayButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e){
                overlayOn = !overlayOn;
                overlayButton.setIcon(scaleIcon(loadIcon("/botoes/botoes/botao_overlay_" + (overlayOn?"on":"off") + ".png")));
                if(overlayOn){
                    overlayWindow.setLocation(getX() - 360 + getWidth() + 10, getY() + 10);
                    overlayWindow.updateOverlay(isOn, globalClickCount, toggleKeyName);
                    overlayWindow.setVisible(true);
                } else {
                    overlayWindow.setVisible(false);
                }
            }
        });

        // Right Click Button
        rightClickButton = new JLabel(scaleIcon(loadIcon("/botoes/botoes/botao_right_off.png")));
        rightClickButton.setBounds(33,210,rightClickButton.getIcon().getIconWidth(), rightClickButton.getIcon().getIconHeight());
        panel.add(rightClickButton);
        rightClickButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e){
                rightClickOn = !rightClickOn;
                rightClickButton.setIcon(scaleIcon(loadIcon("/botoes/botoes/botao_right_" + (rightClickOn?"on":"off") + ".png")));
            }
        });

        // Toggle Button
        toggleButton = new JLabel(scaleIcon(loadIcon("/botoes/botoes/botao_toggle.png")));
        toggleButton.setBounds(33,370,toggleButton.getIcon().getIconWidth(), toggleButton.getIcon().getIconHeight());
        panel.add(toggleButton);
        toggleButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e){
                promptForToggleKey();
            }
        });

        // Scroll Button e ScrollBar
        scrollButton = new JLabel(scaleIcon(loadIcon("/botoes/botoes/botao_scroll.png")));
        scrollButton.setBounds(33,130,scrollButton.getIcon().getIconWidth(), scrollButton.getIcon().getIconHeight());
        panel.add(scrollButton);

        scrollBar = new JLabel(scaleIcon(loadIcon("/botoes/scroll/scroll.png")));
        scrollBar.setBounds(30,85,scrollBar.getIcon().getIconWidth(), scrollBar.getIcon().getIconHeight());
        panel.add(scrollBar);
        panel.setComponentZOrder(scrollBar, 1);

        scrollMark = new RotatingLabel(scaleIcon(loadIcon("/botoes/scroll/marca_scroll.png")));
        int markX = scrollBar.getX() + (int)((scrollValue/50.0) * (scrollBar.getWidth() - scrollMark.getIcon().getIconWidth()));
        int markY = scrollBar.getY() + scrollBar.getHeight()/2 - scrollMark.getIcon().getIconHeight()/2;
        scrollMark.setBounds(markX, markY, scrollMark.getIcon().getIconWidth(), scrollMark.getIcon().getIconHeight());
        panel.add(scrollMark);
        panel.setComponentZOrder(scrollMark, 0);

        // Janela overlay
        overlayWindow = new OverlayWindow();
        if (overlayOn) {
            overlayWindow.setLocation(getX() - 360 + getWidth() + 10, getY() + 10);
            overlayWindow.setVisible(true);
        }

        // Fontes e labels
        Font minhaFonte;
        try {
            minhaFonte = Font.createFont(Font.TRUETYPE_FONT,
                    getClass().getResourceAsStream("/fontes/Coiny-Regular.ttf"));
            minhaFonte = minhaFonte.deriveFont(Font.BOLD, 20f);
        } catch (Exception e) {
            e.printStackTrace();
            minhaFonte = new Font("Arial", Font.BOLD, 20);
        }
        Font pequenaFonte = minhaFonte.deriveFont(Font.BOLD, 16f);
        Font maiorFonte = minhaFonte.deriveFont(Font.BOLD, 48f);
        Font cpsFonte = minhaFonte.deriveFont(Font.BOLD, 82f);
        Font menorFonte = minhaFonte.deriveFont(Font.BOLD, 12f);

        JLabel overlayLabel = new JLabel("Overlay");
        overlayLabel.setFont(minhaFonte);
        overlayLabel.setForeground(Color.black);
        overlayLabel.setBounds(60, 170, 150, 30);
        panel.add(overlayLabel);

        JLabel rightClickLabel = new JLabel("Right click");
        rightClickLabel.setFont(minhaFonte);
        rightClickLabel.setForeground(Color.black);
        rightClickLabel.setBounds(60, 210, 150, 30);
        panel.add(rightClickLabel);

        JLabel cpsLabel = new JLabel("CPS Range");
        cpsLabel.setFont(minhaFonte);
        cpsLabel.setForeground(Color.black);
        cpsLabel.setBounds(60, 130, 150, 30);
        panel.add(cpsLabel);

        JLabel toggleLabel1 = new JLabel("Toggle");
        toggleLabel1.setFont(pequenaFonte);
        toggleLabel1.setForeground(Color.black);
        toggleLabel1.setBounds(110, 368, 100, 20);
        panel.add(toggleLabel1);

        JLabel toggleLabel2 = new JLabel("button");
        toggleLabel2.setFont(pequenaFonte);
        toggleLabel2.setForeground(Color.black);
        toggleLabel2.setBounds(110, 383, 100, 20);
        panel.add(toggleLabel2);

        toggleKeyLabel = new JLabel(toggleKeyName);
        toggleKeyLabel.setFont(pequenaFonte);
        toggleKeyLabel.setForeground(Color.white);
        toggleKeyLabel.setBounds(43, 372, 140, 30);
        panel.add(toggleKeyLabel);
        panel.setComponentZOrder(toggleKeyLabel, 0);

        JLabel mtClickerLabel = new JLabel("MTclicker");
        mtClickerLabel.setFont(maiorFonte);
        mtClickerLabel.setForeground(Color.black);
        mtClickerLabel.setBounds(50, 20, 300, 50);
        panel.add(mtClickerLabel);

        cpsCounterLabel = new JLabel("00");
        cpsCounterLabel.setFont(cpsFonte);
        cpsCounterLabel.setForeground(Color.black);
        cpsCounterLabel.setBounds(230, 90, 250, 100);
        panel.add(cpsCounterLabel);

        JTextField cpsField = new JTextField(String.valueOf(scrollValue));
        cpsField.setFont(menorFonte);
        cpsField.setHorizontalAlignment(JTextField.CENTER);
        cpsField.setBounds(scrollButton.getX() + 1, scrollButton.getY(), scrollButton.getWidth(), scrollButton.getHeight() + 2);
        cpsField.setOpaque(false);
        cpsField.setForeground(Color.white);
        cpsField.setBorder(BorderFactory.createLineBorder(Color.black, 1));
        panel.add(cpsField);
        panel.setComponentZOrder(cpsField, 0);
        cpsField.setEditable(false);

        ((AbstractDocument) cpsField.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if(string==null) return;
                StringBuilder sb = new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
                sb.insert(offset,string);
                if(sb.toString().matches("\\d{0,2}")) super.insertString(fb,offset,string,attr);
            }
            @Override
            public void replace(FilterBypass fb,int offset,int length,String text,AttributeSet attrs) throws BadLocationException{
                if(text==null) text="";
                StringBuilder sb = new StringBuilder(fb.getDocument().getText(0,fb.getDocument().getLength()));
                sb.replace(offset, offset+length, text);
                if(sb.toString().matches("\\d{0,2}")) super.replace(fb,offset,length,text,attrs);
            }
        });

        cpsField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e){
                cpsField.setEditable(true);
                cpsField.requestFocusInWindow();
                cpsField.selectAll();
            }
        });

        cpsField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e){
                cpsField.setEditable(false);
                cpsField.setText(String.valueOf(scrollValue));
            }
        });

        cpsField.addActionListener(e -> {
            try {
                int value = Integer.parseInt(cpsField.getText());
                if(value<0) value=0;
                if(value>50) value=50;
                scrollValue=value;
                cpsField.setText(String.valueOf(scrollValue));
                int minX = scrollBar.getX();
                int maxX = scrollBar.getX() + scrollBar.getWidth() - scrollMark.getWidth();
                int newX = minX + (int)((scrollValue/50.0)*(maxX-minX));
                scrollMark.setLocation(newX, scrollMark.getY());
                scrollMark.setRotation(scrollValue*3.6);
            } catch(Exception ex){
                cpsField.setText(String.valueOf(scrollValue));
            }
            cpsField.setEditable(false);
        });

        scrollMark.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e){
                int newX = scrollMark.getX() + e.getX() - scrollMark.getWidth()/2;
                int minX = scrollBar.getX();
                int maxX = scrollBar.getX() + scrollBar.getWidth() - scrollMark.getWidth();
                if(newX<minX) newX=minX;
                if(newX>maxX) newX=maxX;
                scrollMark.setLocation(newX, scrollMark.getY());
                scrollValue = (int)(50.0*(newX-minX)/(maxX-minX));
                cpsField.setText(String.valueOf(scrollValue));
                scrollMark.setRotation(scrollValue*3.6);
            }
        });

        // Arraste da janela
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e){ mouseClickPoint = e.getPoint(); }
        });

        panel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e){
                if(mouseClickPoint!=null){
                    Point location = getLocation();
                    setLocation(location.x+e.getX()-mouseClickPoint.x, location.y+e.getY()-mouseClickPoint.y);
                }
            }
        });

        // Contador global de clicks + overlay
        setupGlobalMouseListener();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void setupGlobalMouseListener() {
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);

        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException e) {
            e.printStackTrace();
            return;
        }

        // Resetador do contador
        Timer resetTimer = new Timer(1000,e->{
            globalClickCount=0;
            cpsCounterLabel.setText("00");
            if(overlayOn) overlayWindow.updateOverlay(isOn, globalClickCount, toggleKeyName);
        });
        resetTimer.start();

        GlobalScreen.addNativeMouseListener(new NativeMouseListener() {
            @Override public void nativeMouseClicked(NativeMouseEvent e){
                globalClickCount++;
                cpsCounterLabel.setText(String.format("%02d",globalClickCount));
                if(overlayOn) overlayWindow.updateOverlay(isOn, globalClickCount, toggleKeyName);
            }
            @Override public void nativeMousePressed(NativeMouseEvent e) {}
            @Override public void nativeMouseReleased(NativeMouseEvent e) {}
        });

        GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
            @Override
            public void nativeKeyPressed(NativeKeyEvent e){
                if(toggleKeyName.equalsIgnoreCase(NativeKeyEvent.getKeyText(e.getKeyCode()))){
                    SwingUtilities.invokeLater(() -> togglePower());
                }
            }
            @Override public void nativeKeyReleased(NativeKeyEvent e) {}
            @Override public void nativeKeyTyped(NativeKeyEvent e) {}
        });
    }

    private void promptForToggleKey() {
        final JDialog dialog = new JDialog(this, "Escolha a tecla", true);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));
        dialog.setSize(320, 120);
        dialog.setLocationRelativeTo(this);

        Image background = loadImage("/janelas/fundo_togglewindow.png");

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (background != null) g.drawImage(background, 0, 0, getWidth(), getHeight(), this);
            }
        };
        panel.setOpaque(false);
        panel.setLayout(new BorderLayout());

        JLabel label = new JLabel("Pressione a tecla desejada", SwingConstants.CENTER);
        label.setFont(toggleKeyLabel.getFont());
        label.setForeground(Color.WHITE);
        panel.add(label, BorderLayout.CENTER);

        dialog.setContentPane(panel);
        dialog.setAlwaysOnTop(true);

        dialog.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dialog.dispose();
                } else {
                    toggleKeyName = KeyEvent.getKeyText(e.getKeyCode());
                    toggleKeyLabel.setText(toggleKeyName);
                    if (overlayOn) overlayWindow.updateOverlay(isOn, globalClickCount, toggleKeyName);
                    dialog.dispose();
                }
            }
        });

        dialog.setFocusable(true);
        dialog.requestFocusInWindow();
        dialog.setVisible(true);
    }

    private void startAnimation(){
        if(animationTimer!=null && animationTimer.isRunning()) return;
        ArrayList<ImageIcon> frames = isOn ? offFrames : onFrames;
        currentFrame=0;
        animationTimer = new Timer(35,null);
        animationTimer.addActionListener(e->{
            powerButton.setIcon(frames.get(currentFrame));
            currentFrame++;
            if(currentFrame>=frames.size()){
                animationTimer.stop();
                isOn=!isOn;
                if(overlayOn) overlayWindow.updateOverlay(isOn, globalClickCount, toggleKeyName);
            }
        });
        animationTimer.start();
    }

    private void togglePower() {
        clickerOn = !clickerOn;
        if(clickerOn) startClicker();
        else stopClicker();
        startAnimation();
        if(overlayOn) overlayWindow.updateOverlay(clickerOn, globalClickCount, toggleKeyName);
    }

    private void startClicker(){
        if(clickTimer!=null && clickTimer.isRunning()) return;
        int cps = scrollValue > 0 ? scrollValue : 1;
        int delay = (int) (1000.0 / cps);
        clickTimer = new Timer(delay, e -> {
            if(clickerOn){
                if (rightClickOn) {
                    robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
                    robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
                } else {
                    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                }
            }
        });
        clickTimer.start();
    }

    private void stopClicker(){
        if(clickTimer!=null){
            clickTimer.stop();
            clickTimer = null;
        }
    }

    // --- MÉTODOS DE CARREGAMENTO SEGURO ---
    private ImageIcon loadIcon(String path){
        URL resource = getClass().getResource(path);
        if(resource == null){
            System.err.println("⚠ Imagem não encontrada: " + path);
            return new ImageIcon(new BufferedImage(50,50,BufferedImage.TYPE_INT_ARGB));
        }
        return new ImageIcon(resource);
    }

    private ImageIcon scaleIcon(ImageIcon icon){
        if(icon==null) return new ImageIcon(new BufferedImage(50,50,BufferedImage.TYPE_INT_ARGB));
        int w=(int)(icon.getIconWidth()*1.5);
        int h=(int)(icon.getIconHeight()*1.5);
        Image img = icon.getImage().getScaledInstance(w,h,Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

    private Image loadImage(String path){
        URL resource = getClass().getResource(path);
        if(resource == null){
            System.err.println("⚠ Imagem não encontrada: " + path);
            return new BufferedImage(100,100,BufferedImage.TYPE_INT_ARGB);
        }
        return new ImageIcon(resource).getImage();
    }

    static class RotatingLabel extends JLabel{
        private double angle = 0;
        public RotatingLabel(ImageIcon icon){ super(icon); }
        public void setRotation(double angle){ this.angle=angle; repaint(); }
        @Override
        protected void paintComponent(Graphics g){
            Graphics2D g2 = (Graphics2D) g.create();
            int cx = getWidth()/2;
            int cy = getHeight()/2;
            g2.rotate(Math.toRadians(angle),cx,cy);
            super.paintComponent(g2);
            g2.dispose();
        }
    }

    public static void main(String[] args){
        SwingUtilities.invokeLater(MTclicker::new);
    }
}
