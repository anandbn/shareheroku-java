package models;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import play.db.jpa.Model;

public class AppCloneRequest  extends Model{
	@Id @GeneratedValue
	public long reqId;

	public String emailAddress;
	public String gitUrl;

	public AppCloneRequest(String emailAddress, String gitUrl) {
		super();
		this.emailAddress = emailAddress;
		this.gitUrl = gitUrl;
	}
}
