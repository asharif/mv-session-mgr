/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.orphanware.mv.sessionmgr;

import com.tigr.mvapi.utility.MVString;
import java.sql.SQLException;

/**
 *
 * @author arash
 */
public class MVResultSet {

    private String resultStr;
    private String[] rows;
    private int cursor = 0;

    public String getResultStr(){
        return this.resultStr;
    }

    public MVResultSet(String resultStr) {

        this.resultStr = resultStr;
        parseRows();

    }

    private void parseRows() {

        rows = resultStr.split("\\|");
        


    }

    public String getAttrString(int attrIndex) {



        String[] attributes = this.rows[cursor].split(MVString.AM);
        return attributes[attrIndex];



    }

    public String getValueString(int attrIndex, int valueIndex) {

        String[] attributes = this.rows[cursor].split(MVString.AM);
        String[] values = attributes[attrIndex].split(MVString.VM);
        return values[valueIndex];
    }


    public String getSubValueString(int attrIndex, int valueIndex, int sValueIndex) {

        String[] attributes = this.rows[cursor].split(MVString.AM);
        String[] values = attributes[attrIndex].split(MVString.VM);
        String[] subvals = values[valueIndex].split(MVString.SVM);
        return subvals[sValueIndex];
    }

    public int getNumRows() {

        if(getNumAttributes() == 1 && getAttrString(0).equals(" "))
            return 0;
        return this.rows.length;
    }

    public int getNumAttributes() {

        String[] attributes = this.rows[cursor].split(MVString.AM);
        return attributes.length;
    }

    public int getNumValues(int attrIndex) {

        String[] attributes = this.rows[cursor].split(MVString.AM);
        String[] values = attributes[attrIndex].split(MVString.VM);
        return values.length;
    }

    public int getNumSubValues(int attrIndex, int valueIndex) {

        String[] attributes = this.rows[cursor].split(MVString.AM);
        String[] values = attributes[attrIndex].split(MVString.VM);
        String[] subvals = values[valueIndex].split(MVString.SVM);
        return subvals.length;
    }

    public String getRow(int i) {

        return rows[i];
    }



    public boolean next() throws SQLException {

        int rowCount = this.rows.length-1;

        if (this.cursor < rowCount) {

            ++this.cursor;
            return true;

        } else {
            return false;
        }

    }

    

    public boolean isFirst() throws SQLException {


        if (this.cursor == 0) {
            return true;
        } else {
            return false;
        }


    }

    public boolean isLast() throws SQLException {



        int last = this.rows.length - 1;
        if (this.cursor == last) {
            return true;
        } else {
            return false;
        }

    }


    public boolean first() throws SQLException {

        if (this.cursor == 0) {
            return true;
        } else {

            this.cursor = 0;
            return false;
        }


    }

    
    public boolean last() throws SQLException {



        int last = this.rows.length - 1;
        if (this.cursor == last) {
            return true;
        } else {

            this.cursor = last;
            return false;
        }


    }

    
    public int getRow() throws SQLException {


        return cursor;

    }

   

    public boolean previous() throws SQLException {



        if (this.cursor >= 0) {

            this.cursor--;
            return true;

        } else {
            return false;
        }
    }

    
}
