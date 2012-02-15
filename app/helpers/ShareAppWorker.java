package helpers;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class ShareAppWorker {
	private static JedisPoolFactory poolFactory = new JedisPoolFactory();
    
	public static void main(String[] args) throws InterruptedException {
        JedisPool pool = poolFactory.getPool();
        Jedis jedis = pool.getResource();
        
        while(true) {
            String appToClone = jedis.lpop("queue");  
            String[] tokens = appToClone.split("|");
            String emailAddress = tokens[0];
            String gitUrl=tokens[1];
            System.out.println(String.format("Received app to clone: Owner Emai:%s,Git URL:%s",emailAddress,gitUrl));
            Thread.sleep(200);
        }
        
        
    }
}
