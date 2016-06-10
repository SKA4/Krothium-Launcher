package kmlk;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @website http://krotium.com
 * @author DarkLBP
 */

public class Profiles {
    private final Map<String, Profile> profiles = new HashMap();
    private final Console console;
    private final Kernel kernel;
    private Versions versions;
    private Profile selected;
    
    public Profiles(Kernel instance)
    {
        this.console = instance.getConsole();
        this.kernel = instance;
    }
    public void add(String name, Profile p)
    {
        if (!profiles.containsKey(name))
        {
            profiles.put(name, p);
            console.printInfo("Profile " + name + " loaded.");
        }
        else
        {
            console.printError("Profile " + name + " duplicated! Trying with undercore.");
            this.add(name + "_", p);
        }
    }
    public void fetchProfiles()
    { 
        console.printInfo("Fetching profiles.");
        this.versions = kernel.getVersions();
        File launcherProfiles = this.kernel.getConfigFile();
        if (launcherProfiles.exists())
        {
            try
            {
                JSONObject root = new JSONObject(Utils.readURL(launcherProfiles.toURI().toURL()));
                JSONObject ples = root.getJSONObject("profiles");
                Set keys = ples.keySet();
                Iterator it = keys.iterator();
                Profile first = null;
                while (it.hasNext())
                {
                    String key = it.next().toString();
                    JSONObject o = ples.getJSONObject(key);
                    String name = null;
                    Version ver = null;
                    File gameDir = null;
                    File javaDir = null;
                    String javaArgs = null;
                    Map<String, Integer> resolution = new HashMap();
                    LauncherVisibility visibility = null;
                    if (o.has("name"))
                    {
                        name = o.getString("name");
                    }
                    if (o.has("allowedReleaseTypes"))
                    {
                        JSONArray a = o.getJSONArray("allowedReleaseTypes");
                        versions.clearAllowList();
                        if (a.length() == 0)
                        {
                            versions.allowType(VersionType.RELEASE);
                        }
                        else
                        {
                            for (int i = 0; i < a.length(); i++)
                            {
                                try
                                {
                                    versions.allowType(VersionType.valueOf(a.getString(i).toUpperCase()));
                                }
                                catch (Exception ex)
                                {
                                    console.printError(a.get(i) + " version type does not exist.");
                                }
                            }
                        }    
                    }
                    else
                    {
                        versions.allowType(VersionType.RELEASE);
                    }
                    if (o.has("lastVersionId"))
                    {
                        ver = versions.getVersionByName(o.getString("lastVersionId"));
                    }
                    if (o.has("gameDir"))
                    {
                        gameDir = new File(o.getString("gameDir"));
                    }
                    if (o.has("javaDir"))
                    {
                        javaDir = new File(o.getString("javaDir"));
                    }
                    if (o.has("javaArgs"))
                    {
                        javaArgs = o.getString("javaArgs");
                    }
                    if (o.has("resolution"))
                    {
                        JSONObject res = o.getJSONObject("resolution");
                        if (res.has("width") && res.has("height"))
                        {
                            resolution.put("width", res.getInt("width"));
                            resolution.put("height", res.getInt("height"));
                        }
                        else
                        {
                            console.printError("Profile " + ((name != null) ? name : "UNKNOWN") + " has an invalid resolution.");
                        }
                    }
                    if (o.has("launcherVisibilityOnGameClose"))
                    {
                        String vis = o.getString("launcherVisibilityOnGameClose");
                        if (vis != null)
                        {
                            if (vis.length() >= 4)
                            {
                                if (vis.startsWith("close"))
                                {
                                    visibility = LauncherVisibility.CLOSE;
                                }
                                else if (vis.startsWith("hide"))
                                {
                                    visibility = LauncherVisibility.HIDE;
                                }
                                else if (vis.startsWith("keep"))
                                {
                                    visibility = LauncherVisibility.KEEP;
                                }
                            }
                        }
                    }
                    if (name != null)
                    {
                        Profile p = new Profile(kernel, name, ver, gameDir, javaDir, javaArgs, resolution, visibility);
                        if (first == null)
                        {
                            first = p;
                        }
                        this.add(name, p);
                    }
                    else
                    {
                        console.printError("Invalid profile found: " + name);
                    }
                }
                if (this.count() > 0)
                {
                    String selProfile = root.getString("selectedProfile");
                    if (this.profiles.containsKey(selProfile))
                    {
                        console.printInfo("Profile " + selProfile + " marked as selected.");
                        this.selected = this.profiles.get(selProfile);
                    }
                    else
                    {
                        console.printError("Invalid profile selected! Using first loaded (" + first.getName() + ")");
                        this.selected = first;
                    }
                    this.selected.getVersion().prepare();
                }
                else
                {
                    this.createDefaultProfile();
                    this.selected = this.getProfileByName("(Default)");
                }
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                console.printError("Failed to fetch profiles.");
            }
        }
        else
        {
            console.printError("Launcher profiles file not found. Using defaults.");
            this.createDefaultProfile();
        }
    }
    private void createDefaultProfile()
    {
        this.add("(Default)", new Profile(kernel, "(Default)", null, null, null, null, null, null));   
    }
    public Profile getProfileByName(String pName)
    {
        if (profiles.containsKey(pName))
        {
            return profiles.get(pName);
        }
        return null;
    }
    public int count()
    {
        return profiles.size();
    }
    public Profile getSelected()
    {
        return this.selected;
    }
    public JSONObject toJSON()
    {
        JSONObject o = new JSONObject();
        JSONObject profiles = new JSONObject();
        Set s = this.profiles.keySet();
        Iterator it = s.iterator();
        while (it.hasNext())
        {
            String key = it.next().toString();
            Profile p = this.profiles.get(key);
            JSONObject prof = new JSONObject();
            prof.put("name", p.getName());
            if (p.hasGameDir())
            {
                prof.put("gameDir", p.getGameDir().toString());
            }
            if (p.hasVersion())
            {
                prof.put("lastVersionId", p.getVersion().getID());
            }
            if (p.hasJavaDir())
            {
                prof.put("javaDir", p.getJavaDir().toString());
            }
            if (p.hasJavaArgs())
            {
                prof.put("javaArgs", p.getJavaArgs());
            }
            if (p.hasResolution())
            {
                JSONObject res = new JSONObject();
                res.put("width", p.getResolutionWidth());
                res.put("height", p.getResolutionHeight());
                prof.put("resolution", res);
            }
            List<VersionType> allowed = versions.getAllowedTypes();
            if (allowed.size() > 0)
            {
                JSONArray at = new JSONArray();
                for (VersionType t : allowed)
                {
                    at.put(t.name().toLowerCase());
                }
                prof.put("allowedReleaseTypes", at);
            }
            if (p.hasVisibility())
            {
                String output = null;
                switch (p.getVisibility())
                {
                    case CLOSE:
                       output = "close launcher when game starts";
                       break;
                    case HIDE:
                        output = "hide launcher and re-open when game closes";
                        break;
                    case KEEP:
                        output = "keep the launcher open";
                        break;
                }
                if (output != null)
                {
                    prof.put("launcherVisibilityOnGameClose", output);
                }
            }
            profiles.put(key, prof);
        }
        o.put("profiles", profiles);
        o.put("selectedProfile", this.selected.getName());
        return o;
    }
}