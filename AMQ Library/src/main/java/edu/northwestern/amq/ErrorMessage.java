/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.northwestern.amq;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Brent Billows
 */
@XmlRootElement
public class ErrorMessage {
    
    private int errorCode;
    private String errorMessage;
    
    public ErrorMessage() {
        
    }
    
    public ErrorMessage(String errorMessage, int errorCode) {
        super();
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    /**
     * @return the errorMessage
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @param errorMessage the errorMessage to set
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * @return the errorCode
     */
    public int getErrorCode() {
        return errorCode;
    }

    /**
     * @param errorCode the errorCode to set
     */
    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }
}
