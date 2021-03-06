/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package guiParts;

import Log.Log;
import blokusElements.Board;
import blokusElements.Game;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import network.ServerThread;

/**
 *
 * @author koji
 */
public class BlokusGUI extends javax.swing.JFrame implements Observer {

    public final String TitleString = "BlokusModServer";
    public final String VerString = "0.20b  b160502";
            
    private BlokusPiecePanel[][] boardPanels;
    private TextMessageDialog messageDialog;
    private ServerThread waitThread;
    private Game myGame;
    
    private Log myLog;
    private int MODE;
    private final int PLAY_MODE = 0;
    private final int ANALYZE_MODE = 1;
    private final int PLAY_END = 2;
    private boolean syncLog;
    private boolean actionFlag;
    private LogInputDialog logInputDialog;
    
    public void setServerThread(ServerThread sth) {
        this.waitThread = sth;
    }
  
    public BlokusGUI(Game blokusGame) {
        initComponents();
        this.init();
        this.myGame = blokusGame;
        this.messageDialog.setText("起動が完了しました");
    }
    
    public void showMessageDialog(){
        this.setTitle(TitleString + " ver."+VerString);
        int dw = this.messageDialog.getWidth();
        int dh = this.messageDialog.getHeight();
	Dimension display = Toolkit.getDefaultToolkit().getScreenSize();//ディスプレイサイズ
	int x = this.getX()+(this.getWidth()-dw)/2;//X座標(親フレームの中央になるように)
	int y = this.getY()+(this.getHeight());//Y座標(〃)
	if(display.getWidth()-dw < x) x = (int)(display.getWidth()-dw);
	if(0 > x) x = 0;
	if(display.getHeight()-dh < y) y = (int)(display.getHeight()-dh);
	if(0 > y) y = 0;
	// 位置指定
	this.messageDialog.setLocation(x, y);
        this.messageDialog.setVisible(true);
    }
    
    public void init(){
        this.jPanel1.removeAll();
        if(this.messageDialog == null){
            this.messageDialog = new TextMessageDialog(this,false);
            this.showMessageDialog();
        }
        
        this.boardPanels = new BlokusPiecePanel[Board.BOARDSIZE][Board.BOARDSIZE];
        this.jPanel1.setLayout(new GridLayout(Board.BOARDSIZE+1,Board.BOARDSIZE+1));
        for(int i=-1;i<Board.BOARDSIZE;i++){
            for(int j=-1;j<Board.BOARDSIZE;j++){
                if(i==-1){
                    if(j==-1){
                        this.jPanel1.add(new JLabel(""));
                    } else {
                        JLabel lb = new JLabel(Integer.toString(j));
                        Font font = new Font(lb.getFont().getFontName(), Font.PLAIN, 20);
                        lb.setFont(font);
                        lb.setHorizontalAlignment(JLabel.CENTER);
                        lb.setVerticalAlignment(JLabel.CENTER);
                        this.jPanel1.add(lb);
                    }
                } else {
                    if(j==-1){
                        JLabel lb = new JLabel(Integer.toString(i));
                        Font font = new Font(lb.getFont().getFontName(), Font.PLAIN, 20);
                        lb.setFont(font);
                        lb.setHorizontalAlignment(JLabel.CENTER);
                        lb.setVerticalAlignment(JLabel.CENTER);
                        this.jPanel1.add(lb);
                    } else {
                        boardPanels[i][j] = new BlokusPiecePanel();
                        this.jPanel1.add(boardPanels[i][j]);
                    }
                    
                }
            }
        }
        
        this.myLog = new Log();
        this.jLabel16.setText("PLAY MODE");
        this.jButton1.setEnabled(false);
        this.jButton2.setEnabled(false);
        this.jLabel10.setText("0");
        this.jLabel8.setText("0");
        this.MODE = this.PLAY_MODE;
        this.syncLog = false;
        this.actionFlag = false;
        this.jMenuItem3.setEnabled(false);
                
        this.jPanel1.validate();
        this.jPanel1.repaint();
    }
    
    @Override
    public void update(Observable o, Object o1) {
        if(o instanceof Game){
            Game gameboard = (Game)o;
            int[][] bstate = gameboard.getBoardState();
            for(int y=0;y<bstate.length;y++){
                for(int x=0;x<bstate[y].length;x++){
                    this.boardPanels[y][x].setColor(bstate[y][x]);
                }
            }
            //プレイヤー名
            if(gameboard.getPlayerName()[0] == null){
                this.jLabel14.setText("未接続");
            } else {
                this.jLabel14.setText(gameboard.getPlayerName()[0]);
            }
            if(gameboard.getPlayerName()[1] == null){
                this.jLabel4.setText("未接続");
            } else {
                this.jLabel4.setText(gameboard.getPlayerName()[1]);
            }
            //プレイヤー得点
            this.jLabel11.setText(Integer.toString(gameboard.getScore()[0]));
            this.jLabel6.setText(Integer.toString(gameboard.getScore()[1]));
            if(gameboard.getCurrentPlayer() == -1){
                //ゲーム終了
                int[] scores = gameboard.getScore();
                this.MODE = this.PLAY_END;
                if(scores[0] > scores[1]){
                    this.jLabel13.setText("あなたの勝利です。");
                    this.jLabel7.setText("あなたの敗北です。");
                    this.jLabel2.setText("勝者はプレイヤー0（"+gameboard.getPlayerName()[0]+"）でした");
                    this.messageDialog.addText("勝者はプレイヤー0（"+gameboard.getPlayerName()[0]+"）でした");
                } else if(scores[0] < scores[1]){
                    this.jLabel7.setText("あなたの勝利です。");
                    this.jLabel13.setText("あなたの敗北です。");
                    this.jLabel2.setText("勝者はプレイヤー1（"+gameboard.getPlayerName()[1]+"）でした");
                    this.messageDialog.addText("勝者はプレイヤー1（"+gameboard.getPlayerName()[1]+"）でした");
                } else {
                    this.jLabel7.setText("引き分けです。");
                    this.jLabel13.setText("引き分けです。");
                    this.jLabel2.setText("結果は引き分けでした");
                    this.messageDialog.addText("結果は引き分けでした");
                }
                
            } else if(gameboard.getCurrentPlayer() == 0){
                this.jLabel13.setText("あなたの手番です。");
                this.jLabel13.setBackground(new Color(255,153,0));
                this.jLabel7.setText("あいての手を待っています。");
                this.jLabel7.setBackground(new Color(153,255,102));
            } else if(gameboard.getCurrentPlayer() == 1){
                this.jLabel7.setText("あなたの手番です。");
                this.jLabel7.setBackground(new Color(255,153,0));
                this.jLabel13.setText("あいての手を待っています。");
                this.jLabel13.setBackground(new Color(153,255,102));
            }
            
        }
//        if(o instanceof ConnectionManager){
//            
//        }
        if(o1 instanceof String){
            this.messageDialog.addText((String)o1);
        }
        this.repaint();
    }
    
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        resetAll = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem2 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("BlokusMod Server 0.10  b160411");
        addMouseWheelListener(new java.awt.event.MouseWheelListener() {
            public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
                formMouseWheelMoved(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 464, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jLabel6.setBackground(new java.awt.Color(255, 255, 255));
        jLabel6.setFont(new java.awt.Font("MS UI Gothic", 0, 36)); // NOI18N
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel6.setText("0");
        jLabel6.setOpaque(true);

        jLabel3.setText("得点");

        jLabel7.setBackground(new java.awt.Color(153, 255, 102));
        jLabel7.setText("接続待ちです");
        jLabel7.setOpaque(true);

        jLabel4.setBackground(new java.awt.Color(153, 153, 255));
        jLabel4.setFont(new java.awt.Font("MS UI Gothic", 0, 36)); // NOI18N
        jLabel4.setText("未接続");
        jLabel4.setToolTipText("");
        jLabel4.setOpaque(true);

        jLabel1.setText("プレイヤー１");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, 195, Short.MAX_VALUE)
            .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 172, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(jLabel1))
                .addGap(0, 117, Short.MAX_VALUE))
            .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, 195, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel7)
                .addGap(18, 18, 18)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jLabel11.setBackground(new java.awt.Color(255, 255, 255));
        jLabel11.setFont(new java.awt.Font("MS UI Gothic", 0, 36)); // NOI18N
        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel11.setText("0");
        jLabel11.setOpaque(true);

        jLabel12.setText("得点");

        jLabel13.setBackground(new java.awt.Color(255, 153, 0));
        jLabel13.setText("接続待ちです");
        jLabel13.setOpaque(true);

        jLabel14.setBackground(new java.awt.Color(255, 51, 51));
        jLabel14.setFont(new java.awt.Font("MS UI Gothic", 0, 36)); // NOI18N
        jLabel14.setText("未接続");
        jLabel14.setToolTipText("");
        jLabel14.setOpaque(true);

        jLabel15.setText("プレイヤー０");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel11, javax.swing.GroupLayout.DEFAULT_SIZE, 193, Short.MAX_VALUE)
            .addComponent(jLabel13, javax.swing.GroupLayout.DEFAULT_SIZE, 193, Short.MAX_VALUE)
            .addComponent(jLabel14, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel12)
                    .addComponent(jLabel15))
                .addGap(0, 115, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addComponent(jLabel15)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel14)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel13, javax.swing.GroupLayout.PREFERRED_SIZE, 13, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel12)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jLabel2.setFont(new java.awt.Font("MS UI Gothic", 0, 14)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel2.setText("起動が完了しました");
        jLabel2.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jButton1.setText("NEXT");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("PREV");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jLabel5.setText("Turn");

        jLabel8.setFont(new java.awt.Font("Lucida Grande", 0, 24)); // NOI18N
        jLabel8.setText("0");

        jLabel9.setFont(new java.awt.Font("Lucida Grande", 0, 20)); // NOI18N
        jLabel9.setText("/");

        jLabel10.setFont(new java.awt.Font("Lucida Grande", 0, 20)); // NOI18N
        jLabel10.setText("0");

        jLabel16.setFont(new java.awt.Font("MS UI Gothic", 0, 14)); // NOI18N
        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel16.setText("PLAY MODE");
        jLabel16.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jMenu1.setText("Game");

        resetAll.setText("resetAll");
        resetAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetAllActionPerformed(evt);
            }
        });
        jMenu1.add(resetAll);

        jMenuBar1.add(jMenu1);

        jMenu3.setText("Log");

        jMenuItem4.setText("Analyze Snippet");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItem4);

        jMenuItem1.setText("Analyze txt File");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItem1);

        jMenuItem3.setText("Save As...");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItem3);

        jMenuBar1.add(jMenu3);

        jMenu2.setText("View");

        jMenuItem2.setText("show message dialog");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem2);

        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(59, 59, 59)
                                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel9)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(34, 34, 34))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(16, 16, 16)
                                        .addComponent(jLabel5))
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(7, 7, 7)
                                        .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(59, 59, 59)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jButton1)
                                            .addComponent(jButton2))))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel16, javax.swing.GroupLayout.PREFERRED_SIZE, 177, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(45, 45, 45)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel8)
                            .addComponent(jLabel9)
                            .addComponent(jLabel10))
                        .addGap(45, 45, 45)
                        .addComponent(jButton1)
                        .addGap(18, 18, 18)
                        .addComponent(jButton2)
                        .addGap(49, 49, 49)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel16, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        this.showMessageDialog();
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void resetAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetAllActionPerformed
        this.myGame.deleteObservers();
        this.messageDialog.setVisible(false);
        this.messageDialog = null;
        this.init();
        this.myGame = new Game();
        this.myGame.addObserver(this);
        try {
            if(this.waitThread != null){
                this.messageDialog.resetText();
                this.waitThread.resetAll(16041, this.myGame);
            } else {
                ServerThread sth = new ServerThread(16041,this.myGame);
                sth.waitStart();
            }
            this.messageDialog.addText("リセットが完了しました。");
        } catch (IOException ex) {
            this.messageDialog.addText("リセットに失敗しました。");
        }

    }//GEN-LAST:event_resetAllActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        // TODO add your handling code here:
        if(this.MODE == this.ANALYZE_MODE || this.MODE == this.PLAY_END){
            JFrame frame = new JFrame();
            
            int option = JOptionPane.showConfirmDialog(frame,"現在の盤面を廃棄してもよろしいですか？","ログ解析中",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
            if(option == JOptionPane.NO_OPTION){
                return;
            }

            this.myGame.deleteObservers();
            this.messageDialog.setVisible(false);
            this.messageDialog = null;
            this.init();
            this.myGame = new Game();
            this.myGame.addObserver(this);
            
            try {
                if(this.waitThread != null){
                    this.messageDialog.resetText();
                    this.waitThread.resetAll(16041, this.myGame);
                } else {
                    ServerThread sth = new ServerThread(16041,this.myGame);
                    sth.waitStart();
                }
                this.messageDialog.addText("リセットが完了しました。");
            } catch (IOException ex) {
                this.messageDialog.addText("リセットに失敗しました。");
            }

            }
        
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(".").getAbsoluteFile());
            
            FileNameExtensionFilter filter = new FileNameExtensionFilter("テキストファイル(*.txt))", "txt");
            fileChooser.addChoosableFileFilter(filter);
            fileChooser.setFileFilter(filter);
            
            
            int selected = fileChooser.showOpenDialog(this);
            if (selected == JFileChooser.APPROVE_OPTION){
                if(this.myLog.setLogFile(fileChooser.getSelectedFile()) == 0){
                    this.myLog.logAnalyze(this.myGame);

                    this.jLabel8.setText(String.valueOf(this.myLog.getTurnCount()));
                    this.jLabel10.setText(String.valueOf(this.myLog.getAllTurnCount()));
                    this.jButton2.setEnabled(true);
                    this.jButton1.setEnabled(false);
                    this.jMenuItem3.setEnabled(false);
                    this.jLabel16.setText("ANALYZE MODE");
                    this.MODE = this.ANALYZE_MODE;
                    jLabel2.setText("ファイル読み込みに成功しました。");
                }else{
                    jLabel2.setText("ファイル読み込みに失敗しました。");
                }
            }
        
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        this.nextAction();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // TODO add your handling code here:
        this.prevAction();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void formMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_formMouseWheelMoved
        // TODO add your handling code here:
        if(this.MODE == this.ANALYZE_MODE && this.syncLog == false){
            if(evt.getWheelRotation() > 0){
                if(this.jButton1.isEnabled()){
                    this.nextAction();
                }
            }else{
                if(this.jButton2.isEnabled()){
                    this.prevAction();
                }
            }
        }
    }//GEN-LAST:event_formMouseWheelMoved

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        // TODO add your handling code here:
        String path = new File(".").getAbsoluteFile().getParent();
        File nowFile = new File(path+"/log.txt");
        File saveFile;
        
        try {
            JFileChooser filechooser = new JFileChooser();
            filechooser.setCurrentDirectory(new File(".").getAbsoluteFile());
            
            FileNameExtensionFilter filter = new FileNameExtensionFilter("テキストファイル(*.txt))", "txt");
            filechooser.addChoosableFileFilter(filter);
            filechooser.setFileFilter(filter);
            filechooser.setDialogTitle("名前を付けて保存");

            int selected = filechooser.showSaveDialog(this);
            if (selected == JFileChooser.APPROVE_OPTION){
                saveFile = new File(filechooser.getSelectedFile() + ".txt");
            }else{
                return;
            }

            try {
                BlokusGUI.copyFile(nowFile, saveFile);
            } catch (IOException ex) {
                Logger.getLogger(BlokusGUI.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        } catch (SecurityException e) {
            System.out.println("例外が発生しました。");
            System.out.println(e);
        } catch (NullPointerException e) {
            System.out.println("例外が発生しました。");
            System.out.println(e);
        }
    
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    public static void copyFile(File in, File out) throws IOException {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(),outChannel);
        } 
        catch (IOException e) {
            throw e;
        }
        finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }
    
    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        // TODO add your handling code here:
        if(this.MODE == this.ANALYZE_MODE || this.MODE == this.PLAY_END){
            JFrame frame = new JFrame();

            int option = JOptionPane.showConfirmDialog(frame,"現在の盤面を廃棄してもよろしいですか？","ログ解析中",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
            if(option == JOptionPane.NO_OPTION){
                return;
            }

            this.myGame.deleteObservers();
            this.messageDialog.setVisible(false);
            this.messageDialog = null;
            this.init();
            this.myGame = new Game();
            this.myGame.addObserver(this);
            try {
                if(this.waitThread != null){
                    this.messageDialog.resetText();
                    this.waitThread.resetAll(16041, this.myGame);
                } else {
                    ServerThread sth = new ServerThread(16041,this.myGame);
                    sth.waitStart();
                }
                this.messageDialog.addText("リセットが完了しました。");
            } catch (IOException ex) {
                this.messageDialog.addText("リセットに失敗しました。");
            }

        }
        this.logInputDialog = new LogInputDialog(this,true);
        this.logInputDialog.setVisible(true);
        
        if(this.logInputDialog.getSelectedButton() == 0){
            //出力先を作成する
            String path = new File(".").getAbsoluteFile().getParent();
            File file = new File(path+"/log.txt");

            if(this.myLog.setLogFile(file) == 0){
                this.myLog.logAnalyze(this.myGame);

                this.jLabel8.setText(String.valueOf(this.myLog.getTurnCount()));
                this.jLabel10.setText(String.valueOf(this.myLog.getAllTurnCount()));
                this.jButton2.setEnabled(true);
                this.jButton1.setEnabled(false);
                this.jMenuItem3.setEnabled(true);
                this.jLabel16.setText("ANALYZE MODE");
                this.MODE = this.ANALYZE_MODE;
                jLabel2.setText("ファイル読み込みに成功しました。");
            }else{
                jLabel2.setText("ファイル読み込みに失敗しました。");
            }
        }
        
    }//GEN-LAST:event_jMenuItem4ActionPerformed

    
    private void nextAction(){
        // TODO add your handling code here:
        if(this.actionFlag == true){
            return;
        }
        this.actionFlag = true;
        if(syncLog == false){
            this.syncLog = true;
            ArrayList<String> list;
            //System.out.println(this.myLog.getTurnCount());
            list = this.myLog.nextPut(this.myGame);
            //System.out.println(this.myLog.getTurnCount());
            //System.out.println(list);
            //m.out.println();

            if(this.myLog.getAllTurnCount() == this.myLog.getTurnCount()){            
                this.jLabel8.setText(String.valueOf(this.myLog.getAllTurnCount()));
                this.jButton1.setEnabled(false);
            }else if(!list.get(0).equals("FAILED")){
                //成功時
                this.jLabel8.setText(String.valueOf(this.myLog.getTurnCount()));
                this.jButton2.setEnabled(true);
            }else{
                //失敗時
                jLabel2.setText("これ以上進めません。");
            }
            this.syncLog = false;
        }
        this.actionFlag = false;
      
        
    }
    
    private void prevAction(){
        // TODO add your handling code here:
        if(this.actionFlag == true){
            return;
        }
        this.actionFlag = true;
         if(syncLog == false){
            this.syncLog = true;
            ArrayList<String> list;
            //System.out.println(this.myLog.getTurnCount());
            list = this.myLog.prevPut(this.myGame);
            //System.out.println(this.myLog.getTurnCount());
            //System.out.println(list);
            //System.out.println();
            if(list.get(0).equals("START")){
                this.jLabel8.setText("S");
                this.jButton2.setEnabled(false);
            }else if(!list.get(0).equals("FAILED")){
                //成功時
                this.jLabel8.setText(String.valueOf(this.myLog.getTurnCount()));
                this.jButton1.setEnabled(true);
            }else{
                //失敗時
                jLabel2.setText("これ以上戻れません。");
            }
            this.syncLog = false;
         }
         this.actionFlag = false;
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JMenuItem resetAll;
    // End of variables declaration//GEN-END:variables



}
