/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

import blokusElements.Game;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author koji
 */
public class ServerThread implements Runnable{
    private ServerSocket ssoc;
    private int waitPort;
    private boolean waitng = false;
    private ArrayList<Socket> connetingSocket = new ArrayList<Socket>();
    private ArrayList<ClientConnectionThread> clientThread = new ArrayList<ClientConnectionThread>();
    private Game gameBoard;
    private Thread myThread;
    private Integer lockObject = new Integer("0");
    
    public ServerThread(int port,Game mainGame){
        this.waitPort = port;
        this.gameBoard = mainGame;
        this.connetingSocket = new ArrayList<Socket>();
    }
    
    public void resetAll(int port,Game mainGame) throws IOException{
        this.waitStop();
        this.waitPort = port;
        this.gameBoard = mainGame;
        this.connetingSocket = new ArrayList<Socket>();
        this.waitStart();
    }
    
    public Game getGameBoard() {
        return this.gameBoard;
    }
    public void waitStart() throws IOException{
        this.ssoc = new ServerSocket(this.waitPort);
        this.myThread = new Thread(this);
        myThread.start();
    }
    
    public void waitStop() throws IOException{
        this.waitng = false;
        for(Socket soc:this.connetingSocket){
            try{
                if(!soc.isClosed()){
                    soc.close();
                }
            } catch (IOException ex) {
                this.gameBoard.printMessage("クライアントの切断時にエラーが発生しました。");
            }
        }
        this.clientThread = new ArrayList<ClientConnectionThread>();
        this.connetingSocket = new ArrayList<Socket>();
        this.ssoc.close();
    }
    
    @Override
    public void run() {
        synchronized(lockObject){
            this.waitng = true;
            try {
                InetAddress myHost = InetAddress.getLocalHost();
                this.gameBoard.printMessage("IPアドレス："+myHost.getHostAddress());
            } catch (UnknownHostException ex) {
                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            while(this.waitng){
                this.gameBoard.printMessage("ポート"+this.waitPort+"番で待ち受けを開始しました。");
                try {
                    Socket csoc = this.ssoc.accept();
                    this.connetingSocket.add(csoc);
                    ClientConnectionThread cthread = new ClientConnectionThread(csoc,ServerThread.this);
                    this.clientThread.add(cthread);
                    cthread.createStream();

                } catch (IOException ex) {
                    if(this.waitng){
                        this.gameBoard.printMessage("クライアントの接続エラーが発生しました。");
                    } else {
                        this.gameBoard.printMessage("クライアントの待ち受けが終了しました。");
                    }
                }
            }
        }
    }

    public void printState(){
        if(this.waitng){
            InetAddress myHost;
            try {
                myHost = InetAddress.getLocalHost();
                this.gameBoard.printMessage("IPアドレス："+myHost.getHostAddress());
                this.gameBoard.printMessage("ポート"+this.waitPort+"番で待ち受けを開始しました。");
            } catch (UnknownHostException ex) {
                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    //誰も手を打っていない時に送るメッセージ
    public void played(ClientConnectionThread c) {
        if(this.gameBoard.getGameState() == Game.STATE_WAIT_PLAYER_PLAY){
            for(ClientConnectionThread client:this.clientThread){
                if(client.getPlayerID() == this.gameBoard.getCurrentPlayer()){
                    client.doplay();
                }
            }
        }
    }
    
    //private Pattern PLAYEDMSGPTN = Pattern.compile("401 PLAYED ([0-1]) (1?[0-9]) (1?[0-9]) ([0-5][0-9A-F])-([0-8])");
    //誰かが手を打った時にほかの相手にメッセージを転送するケース
    public void played(ClientConnectionThread aThis, int PlayerID, int x, int y, String PieceID, int PieceDirection) {
        String SendMessage = "401 PLAYED "+PlayerID+" "+x+" "+y+" "+PieceID+"-"+PieceDirection;
        if(this.gameBoard.getGameState() == Game.STATE_WAIT_PLAYER_PLAY){
            for(ClientConnectionThread client:this.clientThread){
                if(!client.equals(aThis)){
                    client.sendMessage(SendMessage);
                }
                if(client.getPlayerID() == this.gameBoard.getCurrentPlayer()){
                    client.doplay();
                }
            }
        }
    }

    //投了を送ってきた場合
    public void playerPassed(ClientConnectionThread aThis, int PlayerID) {
        String SendMessage = "402 PASSED "+PlayerID;
        if(this.gameBoard.getGameState() == Game.STATE_WAIT_PLAYER_PLAY){
            for(ClientConnectionThread client:this.clientThread){
                if(!client.equals(aThis)){
                    client.sendMessage(SendMessage);
                }
                if(client.getPlayerID() == this.gameBoard.getCurrentPlayer()){
                    client.doplay();
                }
            }
        } else if(this.gameBoard.getGameState() == Game.STATE_GAME_END){
            //両方ともパスした場合
           int[] scores = this.gameBoard.getScore();
           int winner = -1;
           if(scores[0] > scores[1]){
               winner = 0;
           } else if(scores[0] < scores[1]){
               winner = 1;
           }
            for(ClientConnectionThread client:this.clientThread){
                client.sendMessage("403 SCORE "+ scores[client.getPlayerID()]);
                client.sendMessage("501 WINNER "+ winner);
                client.sendMessage("502 GAME END");
            }
        }
    }

    public void removeClientThread(ClientConnectionThread aThis) {
        if(this.clientThread.contains(aThis)){
            this.clientThread.remove(aThis);
        }
    }




    
    
    
    
}
