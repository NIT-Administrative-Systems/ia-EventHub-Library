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
public class SimpleResponse {

    private String message;
    
    public SimpleResponse() {
        
    }
    
    public SimpleResponse(String message) {
        super();
        this.message = message;
    }

    /**
     * @return the errorMessage
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }
}