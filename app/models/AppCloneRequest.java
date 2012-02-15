package models;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import play.db.jpa.Model;
@Entity
@Table(name="clone_request")
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
