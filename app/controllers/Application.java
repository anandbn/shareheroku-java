package controllers;

import com.dmurph.tracking.AnalyticsConfigData;
import com.dmurph.tracking.JGoogleAnalyticsTracker;
import com.google.gson.Gson;
import com.heroku.api.App;
import com.heroku.api.HerokuAPI;
import com.heroku.api.connection.HttpClientConnection;
import com.heroku.api.request.login.BasicAuthLogin;

import helpers.EmailHelper;
import helpers.HerokuAppSharingHelper;
import helpers.JedisPoolFactory;
import play.libs.F;
import play.mvc.*;
import play.data.validation.Error;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

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
	        
	      jedis.rpush("queue", String.format("%s|%s",emailAddress,gitUrl));      
	      pool.returnResource(jedis);
       
    	System.out.println(String.format(">>>>>>>>>>Received shareApp call > Email%s,gitUrl:%s",emailAddress,gitUrl));
    }

}
