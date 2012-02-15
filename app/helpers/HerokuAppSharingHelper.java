package helpers;

import com.heroku.api.Heroku;
import com.heroku.api.HerokuAPI;
import com.heroku.api.connection.HttpClientConnection;
import com.heroku.api.App;
import com.heroku.api.request.key.KeyAdd;
import com.heroku.api.request.key.KeyRemove;
import com.heroku.api.request.login.BasicAuthLogin;
import com.heroku.api.request.sharing.SharingAdd;
import com.heroku.api.request.sharing.SharingRemove;
import com.heroku.api.request.sharing.SharingTransfer;
import com.heroku.api.response.Unit;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import play.jobs.Job;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

public class HerokuAppSharingHelper extends Job<App> {

    private static final String SSH_KEY_COMMENT = "share@heroku";
    
    String emailAddress;
    String gitUrl;

    public Throwable exception;

    public HerokuAppSharingHelper(String emailAddress, String gitUrl) {
        this.emailAddress = emailAddress;
        this.gitUrl = gitUrl;
    }

    @Override
    public App doJobWithResult() throws Exception {
        App app = null;

        try {
            HttpClientConnection herokuConnection = new HttpClientConnection(new BasicAuthLogin(System.getenv("HEROKU_USERNAME"), System.getenv("HEROKU_PASSWORD")));
            HerokuAPI herokuAPI = new HerokuAPI(herokuConnection);
            // create an app on heroku (using heroku credentials specified in ${HEROKU_USERNAME} / ${HEROKU_PASSWORD}
            app = herokuAPI.createApp(new App().on(Heroku.Stack.Cedar));

            if (!app.getCreateStatus().equals("complete")) {
                throw new RuntimeException("Could not create the Heroku app");
            }
            log("Got API Key for super User");
            // write the public key to a file
            String fakeUserHome = System.getProperty("java.io.tmpdir") + File.separator + UUID.randomUUID().toString();

            File fakeUserHomeSshDir = new File(fakeUserHome + File.separator + ".ssh");
            fakeUserHomeSshDir.mkdirs();
            log(String.format("Created tmp directory at %s",fakeUserHomeSshDir.getAbsolutePath()));
            JSch jsch = new JSch();
            KeyPair keyPair = KeyPair.genKeyPair(jsch, KeyPair.RSA);
            keyPair.writePrivateKey(fakeUserHomeSshDir.getAbsolutePath() + File.separator + "id_rsa");
            keyPair.writePublicKey(fakeUserHomeSshDir.getAbsolutePath() + File.separator + "id_rsa.pub", SSH_KEY_COMMENT);
            log(String.format("Wrote private/public key pair to file system"));
            
            ByteArrayOutputStream publicKeyOutputStream = new ByteArrayOutputStream();
            keyPair.writePublicKey(publicKeyOutputStream, SSH_KEY_COMMENT);
            publicKeyOutputStream.close();
            String sshPublicKey = new String(publicKeyOutputStream.toByteArray());

            // copy the known_hosts file to the .ssh dir
            File knownHostsFile = new File(getClass().getClassLoader().getResource("known_hosts").getFile());
            FileUtils.copyFileToDirectory(knownHostsFile, fakeUserHomeSshDir);

            // add the key pair to ${HEROKU_USERNAME}
            KeyAdd keyAdd = new KeyAdd(sshPublicKey);
            Unit keyAddResponse = herokuConnection.execute(keyAdd);
            log(String.format("Added Keys to account"));

            if (keyAddResponse == null) {
                throw new RuntimeException("Could not add an ssh key to the user");
            }

            URI sourceRepoUri = new URI(gitUrl);

            // git clone a repo specified in sourceRepoUri to local disk
            File tmpDir = new File(System.getProperty("java.io.tmpdir") + File.separator + "src" +
                    File.separator + sourceRepoUri.getHost() + File.separator + sourceRepoUri.getPath());

            Git gitRepo = null;

            if (!tmpDir.exists()) {
                CloneCommand cloneCommand = new CloneCommand();
                cloneCommand.setURI(sourceRepoUri.toString());
                cloneCommand.setDirectory(tmpDir);
                gitRepo = cloneCommand.call();
            } else {
                File tmpGitDir = new File(tmpDir.getAbsolutePath() + File.separator + ".git");
                Repository repository = new RepositoryBuilder().setGitDir(tmpGitDir).build();
                gitRepo = new Git(repository);
                gitRepo.pull().call();
            }
            log(String.format("Cloned Git Repo"));

            // git push the heroku repo

            gitRepo.getRepository().getFS().setUserHome(new File(fakeUserHome));
            gitRepo.push().setRemote(app.getGitUrl()).call();

            log(String.format("Pushed to Heroku remote Git repo:%s",app.getGitUrl()));
            // share the app with the provided email
            SharingAdd sharingAdd = new SharingAdd(app.getName(), emailAddress);
            Unit sharingAddResponse = herokuConnection.execute(sharingAdd);

            if (sharingAddResponse == null) {
                throw new RuntimeException("Could not add " + emailAddress + " as a collaborator");
            }

            log(String.format("Added '%s' as collaborator",emailAddress));
            // transfer the app to the provided email
            SharingTransfer sharingTransfer = new SharingTransfer(app.getName(), emailAddress);
            Unit sharingTransferResponse = herokuConnection.execute(sharingTransfer);
            log(String.format("Made '%s' the owner",emailAddress));

            if (sharingTransferResponse == null) {
                throw new RuntimeException("Could not transfer the app to " + emailAddress);
            }

            // remove ${HEROKU_USERNAME} as collaborator
            SharingRemove sharingRemove = new SharingRemove(app.getName(), System.getenv("HEROKU_USERNAME"));
            Unit sharingRemoveResponse = herokuConnection.execute(sharingRemove);
            log(String.format("Removed '%s' from collaborators",System.getenv("HEROKU_USERNAME")));

            if (sharingRemoveResponse == null) {
                throw new RuntimeException("Could remove " + System.getenv("HEROKU_USERNAME") + " from the app");
            }

            // remove the key pair from ${HEROKU_USERNAME}
            KeyRemove keyRemove = new KeyRemove(SSH_KEY_COMMENT);
            Unit keyRemoveResponse = herokuConnection.execute(keyRemove);
            
            if (keyRemoveResponse == null) {
                throw new RuntimeException("Could not remove ssh key");
            }
            log(String.format("Removed public keys"));

            // cleanup the fakeUserHome
            new File(fakeUserHome).delete();
            log(String.format("Deleted Temporary file system"));

            return app;

        } catch (IOException e) {
        	log("Exception when sharing an app:"+e.getMessage());
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
           	log("Exception when sharing an app:"+e.getMessage());
            throw new RuntimeException(e);
        } catch (InvalidConfigurationException e) {
           	log("Exception when sharing an app:"+e.getMessage());
            throw new RuntimeException(e);
        } catch (InvalidRemoteException e) {
           	log("Exception when sharing an app:"+e.getMessage());
            throw new RuntimeException(e);
        } catch (WrongRepositoryStateException e) {
           	log("Exception when sharing an app:"+e.getMessage());
            throw new RuntimeException(e);
        } catch (CanceledException e) {
           	log("Exception when sharing an app:"+e.getMessage());
            throw new RuntimeException(e);
        } catch (RefNotFoundException e) {
           	log("Exception when sharing an app:"+e.getMessage());
            throw new RuntimeException(e);
        } catch (DetachedHeadException e) {
           	log("Exception when sharing an app:"+e.getMessage());
            throw new RuntimeException(e);
        } catch (JSchException e) {
           	log("Exception when sharing an app:"+e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onException(Throwable e) {
        this.exception = e;
    }
    
    private void log(String logMsg){
    	System.out.println(String.format(">>>>>>>>>>>%s",logMsg));
    }
}