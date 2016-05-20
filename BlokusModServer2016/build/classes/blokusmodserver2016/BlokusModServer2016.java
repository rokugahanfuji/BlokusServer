/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package blokusmodserver2016;

import blokusElements.Board;
import blokusElements.Game;
import blokusElements.Piece;
import guiParts.BlokusGUI;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import network.ServerThread;

/**
 *
 * @author ktajima
 */
public class BlokusModServer2016 {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(BlokusModServer2016.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(BlokusModServer2016.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(BlokusModServer2016.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedLookAndFeelException ex) {
            Logger.getLogger(BlokusModServer2016.class.getName()).log(Level.SEVERE, null, ex);
        }
        //pieceDebug();
        Game blokusGame = new Game();
        BlokusGUI gui = new BlokusGUI(blokusGame);
        blokusGame.addObserver(gui);
        gui.setVisible(true);
               
        ServerThread sth = new ServerThread(16041,blokusGame);
        try {
            sth.waitStart();
            
    //        blokusGame.setPlayerName(0, "Alice");
    //        blokusGame.setPlayerName(1, "Bob");
    //
    //        blokusGame.play(1, new Piece("59"), 12, 0);
    //        blokusGame.play(1, new Piece("59"), 12, 0);
        } catch (IOException ex) {
        }
        gui.setServerThread(sth);
    }
        
    public static void pieceDebug() {
        ArrayList<String> PieceIDList = Piece.PieceIDList;
        for(String pid:PieceIDList){
            Piece p = new Piece(pid,0);
            for(int i=0;i<8;i++){
                p.setDirction(i);
                System.out.println(pid+"-"+i);
                p.printPiecePattern();
            }
        }
        
    }
    public static void boardDebug() {
        Board b = new Board();
        b.printCurrentBoard();
       
        //プレイヤ0がピースをスタート地点におく
        b.putPiece(0, new Piece("57"), 0, 12);
        b.printCurrentBoard();

        //プレイヤ1がピースをスタート地点でないところにおく　＝　おけない
        b.putPiece(1, new Piece("59"), 8, 0);
        //b.printCurrentBoard();

        //プレイヤ1がピースをスタート地点におく ＝　おける
        b.putPiece(1, new Piece("59"), 12, 0);
        b.printCurrentBoard();

        //プレイヤ0がピースを角１つにつなげて同じピースをおく ＝　おけない
        b.putPiece(0, new Piece("57"), 3, 9);
        //b.printCurrentBoard();
        
        //プレイヤ0がピースをはみ出してしておく ＝　おけない
        b.putPiece(0, new Piece("44"), 2, 12);
        //b.printCurrentBoard();

        //プレイヤ0がピースを重ねておく ＝　おけない
        b.putPiece(0, new Piece("30"), 2, 10);
        //b.printCurrentBoard();
        
        //プレイヤ0がピースを離れておく ＝　おけない
        b.putPiece(0, new Piece("42"), 0, 0);
        //b.printCurrentBoard();

        //プレイヤ0がピースを隣接しておく ＝　おけない
        b.putPiece(0, new Piece("41"), 0, 11);
        //b.printCurrentBoard();

        //プレイヤ0がピースを角１つにつなげておく ＝　おける
        int v = b.putPiece(0, new Piece("5A"), 3, 9);
        b.printCurrentBoard();
        System.out.println(v);

        Piece p = new Piece("56");
        p.setDirction(1);
        p.printPiecePattern();
        
        int v2 = b.putPiece(1, p , 11, 2);
        b.printCurrentBoard();
        System.out.println(v2);

        
    }
}
