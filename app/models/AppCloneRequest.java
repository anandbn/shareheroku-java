package models;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import play.db.jpa.Model;
public class AppCloneRequest  extends Model{
	public String id;

	public String emailAddress;
	public String gitUrl;
	public String status;
	public String appName;
	public String appUrl;
	public String appGitUrl;

	public AppCloneRequest(String emailAddress, String gitUrl,String status) {
		super();
		this.emailAddress = emailAddress;
		this.gitUrl = gitUrl;
		this.status=status;
	}

	public AppCloneRequest() {
		// TODO Auto-generated constructor stub
	}
}
