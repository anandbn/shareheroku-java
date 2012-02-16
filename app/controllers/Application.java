package controllers;

import com.google.gson.Gson;

import helpers.JedisPoolFactory;
import play.mvc.*;
import play.data.validation.Error;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import models.AppCloneRequest;

public class Application extends Controller {
	private static JedisPoolFactory poolFactory = new JedisPoolFactory();
    public static void index() {
        render();
    }

    public static void appGetStarted(String appId){
		JedisPool pool = poolFactory.getPool();
		Jedis jedis = pool.getResource();

		List<String> request = jedis.hmget(appId, "id", "email", "appName",
															"appUrl", "appGitUrl", "status");
		AppCloneRequest app = new AppCloneRequest();
		app.emailAddress = request.get(1);
		app.appName = request.get(2);
		app.appUrl = request.get(3);
		app.appGitUrl = request.get(4);
		app.status=request.get(5);
		render(app);
    }
    public static void shareApp(String emailAddress, String gitUrl) {
		Map<String, Map<String, String>> errors = new HashMap<String, Map<String, String>>();
		errors.put("error", new HashMap<String, String>());

		 JedisPool pool = poolFactory.getPool();
	     Jedis jedis = pool.getResource();
	      AppCloneRequest request = new AppCloneRequest(emailAddress,gitUrl,"New");
	     request.id=UUID.randomUUID().toString();
	      Map<String,String> cloneReq = new HashMap<String,String>();
	      cloneReq.put("id", request.id);
	      cloneReq.put("email",emailAddress);
	      cloneReq.put("appName", "");
	      cloneReq.put("appUrl","");
	      cloneReq.put("appGitUrl", "");
	      cloneReq.put("status","new");
	      
	      jedis.hmset(request.id, cloneReq);
	      System.out.println(String.format("[Requested By:%s] - %s : Added Redis Hash [id=%s] ",emailAddress,"ADDREQ",request.id));
	      String msg = new Gson().toJson(request) ;
	      jedis.rpush("queue",msg);     
	      System.out.println(String.format("[Requested By:%s] - %s : Message to Queue=%s",emailAddress,"CLNREQ",msg));
	      pool.returnResource(jedis);
	      renderJSON(new Gson().toJson(request));
     }
    
    
}
