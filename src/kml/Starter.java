package kml;

import kml.exceptions.AuthenticationException;
import kml.gui.Login;
import kml.gui.Main;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.Properties;

/**
 * @website https://krothium.com
 * @author DarkLBP
 */
class Starter {
    public static void main(String[] args) throws IOException, FontFormatException, ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
        Font font = Font.createFont(Font.TRUETYPE_FONT, Starter.class.getResourceAsStream("/kml/gui/fonts/Minecraftia-Regular.ttf"));
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        ge.registerFont(font);
        if (args.length == 0){
            if (existsResource()){
                bootFromResource(args);
            } else {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                Kernel kernel = new Kernel();
                Console console = kernel.getConsole();
                console.includeTimestamps(true);
                try {
                    HttpsURLConnection con = (HttpsURLConnection)Constants.HANDSHAKE_URL.openConnection();
                    int responseCode = con.getResponseCode();
                    Constants.USE_HTTPS = (responseCode == 204);
                } catch (SSLHandshakeException ex) {
                    Constants.USE_HTTPS = false;
                } catch (IOException ex){
                    Constants.USE_LOCAL = true;
                }
                console.printInfo("Using HTTPS when available? | " + Constants.USE_HTTPS);
                kernel.loadVersions();
                kernel.loadProfiles();
                kernel.loadUsers();
                Authentication a = kernel.getAuthentication();
                if (a.hasSelectedUser()){
                    try{
                        a.refresh();
                    }catch(AuthenticationException ex){
                        console.printError(ex.getMessage());
                    }
                    kernel.saveProfiles();
                }

                if (!kernel.isAuthenticated()){
                    Login login = kernel.getLoginForm();
                    login.setVisible(true);
                } else {
                    Main main = kernel.getMainForm();
                    main.setVisible(true);
                }
            }
        } else if (args.length >= 1){
            String[] stubArgs = new String[args.length - 1];
            if (args.length > 1){
                System.arraycopy(args, 1, stubArgs, 0, args.length - 1);
            }
            if (existsResource()){
                bootFromResource(stubArgs);
            } else {
                File f = new File(args[0]);
                StubLauncher.load(f, stubArgs);
            }
        }
    }
    private static boolean existsResource() {
        try {
            File custom = new File("resource.ini");
            if (custom.exists() && custom.isFile()){
                return true;
            }
            InputStream in = Starter.class.getResourceAsStream("/resource.jar");
            return in != null;
        } catch (Exception ex){
            return false;
        }
    }
    private static void bootFromResource(String[] passedArgs) {
        File custom = new File("resource.ini");
        if (custom.exists() && custom.isFile()){
            Properties p = new Properties();
            try {
                FileInputStream fin = new FileInputStream(custom);
                p.load(fin);
                File resource = new File(p.getProperty("path"));
                StubLauncher.load(resource, passedArgs);
            } catch (Exception ex){
                ex.printStackTrace();
            }
        } else {
            InputStream in = Starter.class.getResourceAsStream("/resource.jar");
            File workingDir = Utils.getWorkingDirectory();
            if (!workingDir.exists() || !workingDir.isDirectory()){
                workingDir.mkdirs();
            }
            File resource = new File(workingDir + File.separator + "resource.jar");
            boolean copy = true;
            if (resource.exists() && resource.isFile()){
                copy = resource.delete();
            }
            if (copy) {
                try {
                    FileOutputStream out = new FileOutputStream(resource);
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    in.close();
                    out.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            StubLauncher.load(resource, passedArgs);
        }
    }
}
