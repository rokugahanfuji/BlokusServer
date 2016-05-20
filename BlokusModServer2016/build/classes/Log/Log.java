/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Log;

import blokusElements.Game;
import blokusElements.Piece;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author fujisawa
 */
public class Log {
    //txtファイル
    private File file;
    
    private String myName = "";
    private int myPlayerID = -1;
    private int opPlayerID = -1;
    
    //打った手のリスト
    private ArrayList<ArrayList<String>> PutList;
    //配列の指している位置
    private int nowPut;
    //配列の最大値。ただし0から数えるものとする（要素番号と同じ）
    private int maxSize;
    
    //メッセージ解析用の正規表現パターン
    private Pattern PLAYEDMSGPTNLOG = Pattern.compile("\\[log\\]405 PLAY (1?[0-9]) (1?[0-9]) ([0-5][0-9A-F])-([0-8])");
    private Pattern PLAYEDMSGPTNRECV = Pattern.compile("\\[recv\\]401 PLAYED ([0-1]) (1?[0-9]) (1?[0-9]) ([0-5][0-9A-F])-([0-8])");
    private Pattern PASSEDMSGPTNLOG = Pattern.compile("\\[log\\]406 PASS");
    private Pattern PASSEDMSGPTNRECV = Pattern.compile("\\[recv\\]402 PASSED ([0-1])");
    private Pattern NAME = Pattern.compile("\\[log\\]101 NAME (.*)");
    //コンストラクタ
    public Log(){
        this.init();
    }
    
    //初期化
    private void init(){
        this.PutList = new ArrayList<ArrayList<String>>();
        this.myName  = "";
        this.myPlayerID = -1;
        this.opPlayerID = -1;
        this.nowPut = 0;
        this.maxSize = 0;
    }
    
    //
    public void logAnalyze(Game game){
        game.setPlayerName(this.myPlayerID,this.myName);
        game.setPlayerName(this.opPlayerID,"ENEMY");
        ArrayList<String> turn;
        
        for (int i = 0; i < maxSize; i++) {
                nextPut(game);
        }
        System.out.println(this.nowPut);
        //0:playerID 1:x 2:y 3:PieceID 4:PieceDirection  
    }
    
    public ArrayList nextPut(Game game){
        if(this.nowPut == maxSize - 1){
            ArrayList<String> turn = this.PutList.get(this.nowPut);
            game.play(Integer.parseInt(turn.get(0)), new Piece(turn.get(3),Integer.parseInt(turn.get(4))), Integer.parseInt(turn.get(1)), Integer.parseInt(turn.get(2)));
            ArrayList<String> re = new ArrayList();
            
            game.AirPlay(turn.get(0).equals("0") ? 1 : 0);
            re.add("END");
            this.nowPut++;

            return re;
        }
        ArrayList<String> turn = this.PutList.get(this.nowPut);
        ArrayList<String> nextTurn = this.PutList.get(this.nowPut + 1);
        
        if(!turn.get(1).equals("PASS")){
            game.play(Integer.parseInt(turn.get(0)), new Piece(turn.get(3),Integer.parseInt(turn.get(4))), Integer.parseInt(turn.get(1)), Integer.parseInt(turn.get(2)));
                        
            if(nextTurn.get(0).equals(turn.get(0))){
                game.AirPlay(turn.get(0).equals("0") ? 1 : 0);
            }
        
        }else{
            game.AirPlay(Integer.parseInt(turn.get(0)));
        }
        
        this.nowPut++;
        
        return this.PutList.get(this.nowPut);
    }
    
    public ArrayList prevPut(Game game){
        if(this.nowPut == 1){
            this.nowPut--;
            ArrayList<String> turn = this.PutList.get(this.nowPut);
            game.AirPlay(turn.get(0).equals("0") ? 1 : 0);
            game.rewind(Integer.parseInt(turn.get(0)), new Piece(turn.get(3),Integer.parseInt(turn.get(4))), Integer.parseInt(turn.get(1)), Integer.parseInt(turn.get(2)));
            game.AirPlay(turn.get(0).equals("0") ? 1 : 0);
            ArrayList<String> re = new ArrayList();
            re.add("START");
            return re;
        }
        
        this.nowPut--;
        
        ArrayList<String> turn = this.PutList.get(this.nowPut);
        ArrayList<String> nextTurn;
        
        if(this.nowPut == maxSize - 1){
            nextTurn = this.PutList.get(this.nowPut);
            System.out.println("max.");
        }else{
            nextTurn = this.PutList.get(this.nowPut + 1);
        }
        
        game.AirPlay(turn.get(0).equals("0") ? 1 : 0);
        
        if(!turn.get(1).equals("PASS")){
            if(nextTurn.get(0).equals(turn.get(0))){
                game.AirPlay(turn.get(0).equals("0") ? 1 : 0);
            }    
            game.rewind(Integer.parseInt(turn.get(0)), new Piece(turn.get(3),Integer.parseInt(turn.get(4))), Integer.parseInt(turn.get(1)), Integer.parseInt(turn.get(2)));
            game.AirPlay(turn.get(0).equals("0") ? 1 : 0);
        }else{
            
        }
        
        return this.PutList.get(this.nowPut);
    }
    
    //ログをセットし、パースまで行う
    public int setLogFile(File f){
        this.file = f;
        if(this.parseTextFile() == 0){
            //成功
            //配列の大きさを確認する
            this.maxSize = this.PutList.size();
            ArrayList<String> turn = new ArrayList();
            this.nowPut = 0;
            turn = this.PutList.get(maxSize-1);
            if(turn.get(1).equals("PASS")){
                this.PutList.remove(maxSize-1);
                maxSize -= 1;
            }
            System.out.println(this.PutList);
            System.out.println("Put NUM :"+this.nowPut);
            System.out.println("Log Size:"+this.PutList.size());
            
        }else{
            //失敗 エラー詳細つけるならここで
            return -1;
        }
        
        return 0;
    }
    
    //txtファイルを手リストに代入する
    private int parseTextFile(){
        try{
            FileReader filereader = new FileReader(this.file);
            BufferedReader br = new BufferedReader(filereader);
            String Name = this.file.getName();
            int point = Name.lastIndexOf(".");
            if(!Name.substring(point+1).equals("txt") || !br.readLine().contains("[recv]100 HELLO")){
                return -1;
            }
            
            String line;
            //一行ずつ最後まで読み込む
            while((line = br.readLine()) != null){
                Matcher mc1 = PLAYEDMSGPTNLOG.matcher(line);
                Matcher mc2 = PLAYEDMSGPTNRECV.matcher(line);
                Matcher mc3 = PASSEDMSGPTNLOG.matcher(line);
                Matcher mc4 = PASSEDMSGPTNRECV.matcher(line);
                Matcher mc5 = NAME.matcher(line);
                
                if(mc1.matches()){//[log]405 PLAYED
                    //1:playerID 2:x 3:y 4:PieceID 5:PieceDirection  
                    ArrayList<String> turn = new ArrayList<String>();
                    turn.add(String.valueOf(this.myPlayerID));
                    for (int i = 1; i < 5; i++) {
                        turn.add(mc1.group(i));
                    }
                    this.PutList.add(turn);
                } else if(mc2.matches()){//[recv]401 PLAYED
                    //0:playerID 1:x 2:y 3:PieceID 4:PieceDirection  
                    ArrayList<String> turn = new ArrayList<String>();
                    for (int i = 1; i < 6; i++) {
                        turn.add(mc2.group(i));
                    }
                    this.PutList.add(turn);
                    
                } else if(mc3.matches()){//[log]406 PASS
                    ArrayList<String> turn = new ArrayList<String>();
                    turn.add(String.valueOf(this.myPlayerID));
                    turn.add("PASS");
                    this.PutList.add(turn);
                } else if(mc4.matches()){//[recv]402 PASSED
                    ArrayList<String> turn = new ArrayList<String>();
                    turn.add(String.valueOf(this.opPlayerID));
                    turn.add("PASS");
                    this.PutList.add(turn);
                } else if(mc5.matches()){//名前
                    this.myName = mc5.group(1);
                } else if(line.equals("[recv]102 PLAYERID 0")){
                    //先手の場合
                    this.myPlayerID = 0;
                    this.opPlayerID = 1;
                } else if(line.equals("[recv]102 PLAYERID 1")){
                    //後手の場合
                    this.myPlayerID = 1;
                    this.opPlayerID = 0;
                } 
            }
            br.close();
            
          }catch(FileNotFoundException e){
            System.out.println(e);
          }catch(IOException e){
            System.out.println(e);
            return -1;
          }
        
        //成功
        return 0;
    }     
    
    public int getTurnCount(){
        return nowPut;
    }
    
    public int getAllTurnCount(){
        return maxSize;
    }
    
    private int reversePlayerID(int ID){
        if(ID == 0){
            return 1;
        }else{
            return 0;
        }
    }
}
