/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

import blokusElements.Board;
import blokusElements.Game;
import blokusElements.Piece;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author koji
 */
public class ClientConnectionThread implements Runnable {
    private Socket connectedSocket;
    private String ID;
    private int PlayerID = -1;
    private BufferedReader reader;
    private PrintWriter writer;
    private final ServerThread HostServer;
    
    public ClientConnectionThread(Socket csoc,ServerThread server){
        this.connectedSocket = csoc;
        this.HostServer = server;
        this.ID = csoc.getInetAddress().getHostAddress();
    }
    
    public void createStream() throws IOException{
        this.reader = new BufferedReader(new InputStreamReader(this.connectedSocket.getInputStream(),"UTF-8"));
        this.writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(this.connectedSocket.getOutputStream(),"UTF-8")));
        
        Thread msgwait = new Thread(this);
        msgwait.start();
        this.HostServer.getGameBoard().printMessage(this.ID+"が接続しました。");
        this.sendMessage("100 HELLO");
    }
    
    public String getAddress(){
        return this.ID;
    }
    
    /** クライアントへのメッセージ送信 */
    public void sendMessage(String message){
        if(this.writer != null){
            this.writer.println(message);
            this.writer.flush();
        }
    }
    
    private Pattern MSGPTN = Pattern.compile("([0-9]+) (.*)");
    private Pattern NAMEMSGPTN = Pattern.compile("101 NAME (.*)");
    private Pattern PLAYMSGPTN = Pattern.compile("405 PLAY (1?[0-9]) (1?[0-9]) ([0-5][0-9A-F])-([0-8])");
    /** クライアントからのメッセ―ジ到着 */
    public void getMessage(String message){
        message = message.trim();
        //this.mainFiled.addMessage(this.ID+":"+message);
        synchronized(this.HostServer){
            //終了処理
            if(message.toUpperCase().equals("203 EXIT")){
                sendMessage("200 OK");
                try {
                    this.connectedSocket.close();
                    this.HostServer.removeClientThread(this);
                } catch (IOException ex) {
                    this.HostServer.getGameBoard().printMessage(this.ID+"が切断時にエラーが発生しました");
                    this.HostServer.removeClientThread(this);
                }
                return;
            }

            //メッセージ解析
            Matcher mc = MSGPTN.matcher(message);
            if(mc.matches()){
                int num = Integer.parseInt(mc.group(1));
                if(this.HostServer.getGameBoard().getGameState() == Game.STATE_WAIT_PLAYER_CONNECTION){
                    if(num == 101){
                        //新規ユーザの接続
                        Matcher nmc = NAMEMSGPTN.matcher(message);
                        if(nmc.matches()){
                            String name = nmc.group(1);
                            if(this.PlayerID == -1){
                                this.PlayerID = this.HostServer.getGameBoard().setPlayerName(name);
                                if(this.PlayerID == 0 || this.PlayerID == 1){
                                    sendMessage("102 PLAYERID "+ this.PlayerID);
                                    this.HostServer.played(this);
                                } else{
                                    sendMessage("301 ROOM IS FULL");
                                }
                            } else {
                                sendMessage("302 ALL REDAY REGSTERD");
                            }
                        } else {
                            sendMessage("300 MESSAGE SYNTAX ERROR");
                        }
                    } else {
                        this.sendMessage("300 MESSAGE SYNTAX ERROR");
                    }
                } else if(this.HostServer.getGameBoard().getGameState() == Game.STATE_WAIT_PLAYER_PLAY){
                    if(num == 400){
                        //ボード状態の取得
                        sendMessage("201 MULTILINE");
                        int[][] list = this.HostServer.getGameBoard().getBoardState();
                        for(int y=0;y<Board.BOARDSIZE;y++){
                            StringBuilder sbuf = new StringBuilder();
                            for(int x=0;x<Board.BOARDSIZE;x++){
                                sbuf.append(list[y][x]);
                                sbuf.append(",");
                            }
                            sbuf.deleteCharAt(sbuf.lastIndexOf(","));
                            sendMessage(sbuf.toString());
                        }
                        sendMessage("202 LINEEND");
                    } else if(num == 405){
                        if (this.HostServer.getGameBoard().getCurrentPlayer() != this.PlayerID){
                            //自分の手ではない
                            this.sendMessage("304 NOT YOURE TURN"); 
                        } else {
                            Matcher nmc = PLAYMSGPTN.matcher(message);
                            if(nmc.matches()){
                                int x = Integer.parseInt(nmc.group(1));
                                int y = Integer.parseInt(nmc.group(2));
                                String PieceID = nmc.group(3);
                                int PieceDirection = Integer.parseInt(nmc.group(4));
                                boolean played = this.HostServer.getGameBoard().play(this.PlayerID, new Piece(PieceID,PieceDirection), x, y);

                                if(played == true){
                                    this.sendMessage("200 OK");
                                    //もう一人の相手にメッセージを送るようにサーバに要求
                                    this.HostServer.played(this,this.PlayerID,x,y,PieceID,PieceDirection);
                                } else {
                                    //移動方法が間違っている
                                   this.sendMessage("303 PIECE COULD NOT PUT"); 
                                   this.sendMessage("404 DOPLAY");
                                }
                            } else {
                               this.sendMessage("300 MESSAGE SYNTAX ERROR");
                               this.sendMessage("404 DOPLAY");
                            }
                        }
                    } else if(num == 406){
                        //パスした場合
                        if (this.HostServer.getGameBoard().getCurrentPlayer() != this.PlayerID){
                            //自分の手ではない
                            this.sendMessage("304 NOT YOURE TURN"); 
                        } else {
                            this.HostServer.getGameBoard().pass(this.PlayerID);
                            this.sendMessage("200 OK");
                            //もう一人の相手にメッセージを送るようにサーバに要求
                            this.HostServer.playerPassed(this,this.PlayerID);
                        }
                    } else if(num == 407){
                        int[] score = this.HostServer.getGameBoard().getScore();
                        this.sendMessage("408 SCORE "+score[0]+" "+score[1]);
                    } else {
                        sendMessage("300 MESSAGE SYNTAX ERROR");
                    }
                } else if(this.HostServer.getGameBoard().getGameState() == Game.STATE_GAME_END){
                    if(num == 400){
                        //ボード状態の取得
                        sendMessage("201 MULTILINE");
                        int[][] list = this.HostServer.getGameBoard().getBoardState();
                        for(int y=0;y<Board.BOARDSIZE;y++){
                            StringBuilder sbuf = new StringBuilder();
                            for(int x=0;x<Board.BOARDSIZE;x++){
                                sbuf.append(list[y][x]);
                                sbuf.append(",");
                            }
                            sbuf.deleteCharAt(sbuf.lastIndexOf(","));
                            sendMessage(sbuf.toString());
                        }
                        sendMessage("202 LINEEND");
                    } else {
                        this.sendMessage("300 MESSAGE SYNTAX ERROR");
                    }
                }
            } else {
                sendMessage("300 MESSAGE SYNTAX ERROR");
            }
        }
    }
    
    @Override
    public void run() {
        String mssage;
        try {
            while((mssage = this.reader.readLine())!= null){
                this.getMessage(mssage);
            }
            this.HostServer.removeClientThread(this);
            this.HostServer.getGameBoard().printMessage(this.ID+"が切断しました");
        } catch (IOException ex) {
            this.HostServer.getGameBoard().printMessage(this.ID+"が切断しました");
            this.HostServer.removeClientThread(this);
        }
    }

    /** プレイコマンドを送る */
    public void doplay() {
        this.HostServer.getGameBoard().TimerStart(PlayerID);
        this.sendMessage("404 DOPLAY");
    }

    public void gameend(int winner) {
        //TODO 終了処理の追加
        
        this.sendMessage("502 GAMEEND "+winner);
    }

    public int getPlayerID() {
        return this.PlayerID;
    }

    public void closeConnection() throws IOException {
        this.connectedSocket.close();
    }

   
}
