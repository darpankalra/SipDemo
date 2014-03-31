package com.example.android.sip;

import java.io.Serializable;

public class CallInfo implements Serializable {
	private String sipAddr;
	private String callDate;
	private String callDuration;
	private boolean outgoingCall;
	private boolean missedCall;
	
	public boolean isMissedCall() {
		return missedCall;
	}
	public void setMissedCall(boolean missedCall) {
		this.missedCall = missedCall;
	}
	public CallInfo(String sipAddr, String callDate, String callDuration,
			boolean outgoingCall, boolean missedCall) {
		super();
		this.sipAddr = sipAddr;
		this.callDate = callDate;
		this.callDuration = callDuration;
		this.outgoingCall = outgoingCall;
		this.missedCall = missedCall;
	}
	public String getSipAddr() {
		return sipAddr;
	}
	public void setSipAddr(String sipAddr) {
		this.sipAddr = sipAddr;
	}
	public String getCallDate() {
		return callDate;
	}
	public void setCallDate(String callDate) {
		this.callDate = callDate;
	}
	public String getCallDuration() {
		return callDuration;
	}
	public void setCallDuration(String callDuration) {
		this.callDuration = callDuration;
	}
	public String toString()
	{
		return sipAddr;
	}
	public boolean isOutgoingCall() {
		return outgoingCall;
	}
	public void setOutgoingCall(boolean outgoingCall) {
		this.outgoingCall = outgoingCall;
	}
}
