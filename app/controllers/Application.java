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

    public static void shareApp(String emailAddress, String gitUrl) {
		Map<String, Map<String, String>> errors = new HashMap<String, Map<String, String>>();
		errors.put("error", new HashMap<String, String>());

		 JedisPool pool = poolFactory.getPool();
	     Jedis jedis = pool.getResource();
	      AppCloneRequest request = new AppCloneRequest(emailAddress,gitUrl);
	      request.create();
	      String msg = new Gson().toJson(request) ;
	      jedis.rpush("queue",msg);     
	      System.out.println(String.format("[Requested By:%s] - %s : Message to Queue=%s",emailAddress,"CLNREQ",msg));
	      pool.returnResource(jedis);
	      render();
     }
}
