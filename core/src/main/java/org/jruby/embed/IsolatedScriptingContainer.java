package org.jruby.embed;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;

/**
 * the IsolatedScriptingContainer does set GEM_HOME and GEM_PATH and JARS_HOME
 * in such a way that it uses only resources which can be reached with classloader.
 * 
 * GEM_HOME is uri:classloader://META-INF/jruby.home/lib/ruby/gems/shared
 * GEM_PATH is uri:classloader://
 * JARS_HOME is uri:classloader://jars
 * 
 * but whenever you want to set them via {@link #setEnvironment(Map)} this will be honored.
 * 
 * it also comes with OSGi support which allows to add a bundle to LOAD_PATH or GEM_PATH.
 */
public class IsolatedScriptingContainer extends ScriptingContainer {

    private static final String JRUBY_HOME = "/META-INF/jruby.home";

    public IsolatedScriptingContainer()
    {
        this(LocalContextScope.SINGLETON);
    }

    public IsolatedScriptingContainer( LocalContextScope scope,
                                       LocalVariableBehavior behavior )
    {
        this(scope, behavior, true);
    }

    public IsolatedScriptingContainer( LocalContextScope scope )
    {
        this(scope, LocalVariableBehavior.TRANSIENT);
    }

    public IsolatedScriptingContainer( LocalVariableBehavior behavior )
    {
        this(LocalContextScope.SINGLETON, behavior);
    }

    public IsolatedScriptingContainer( LocalContextScope scope,
                                       LocalVariableBehavior behavior,
                                       boolean lazy )
    {
        super(scope, behavior, lazy);

        setLoadPaths( Arrays.asList( "uri:classloader:" ) );

        // setup the isolated GEM_PATH, i.e. without $HOME/.gem/**
        setEnvironment(null);
    }

    @Override
    public void setEnvironment(Map environment) {
        if (environment == null || !environment.containsKey("GEM_PATH") 
                || !environment.containsKey("GEM_HOME")|| !environment.containsKey("JARS_HOME")) {
            Map<String,String> env = environment == null ? new HashMap<String,String>() : new HashMap<String,String>(environment);
            if (!env.containsKey("GEM_PATH")) env.put("GEM_PATH", "uri:classloader://");
            if (!env.containsKey("GEM_HOME")) env.put("GEM_HOME", "uri:classloader:/" + JRUBY_HOME + 
                    "/lib/ruby/gems/shared");
            if (!env.containsKey("JARS_HOME")) env.put("JARS_HOME", "uri:classloader://jars");
            super.setEnvironment(env);
        }
        else {
            super.setEnvironment(environment);
        }
    }

    private Bundle toBundle(String symbolicName) {
        BundleContext context = FrameworkUtil.getBundle(getClass()).getBundleContext();
        Bundle bundle = null;
        for (Bundle b : context.getBundles()) {
            if (b.getSymbolicName().equals(symbolicName)) {
                bundle = b;
                break;
            }
        }
        if (bundle == null ) {
            throw new RuntimeException("unknown bundle: " + symbolicName);
        }
        return bundle;
    }

    private String createUri(Bundle cl, String ref) {
        URL url = cl.getResource( ref );
        if ( url == null && ref.startsWith( "/" ) ) {
            url = cl.getResource( ref.substring( 1 ) );
        }
        if ( url == null ) {
            throw new RuntimeException( "reference " + ref + " not found on classloader " + cl );
        }
        System.err.println("=---" + url.toString().replaceFirst( ref + "$", "" ));
        return "uri:" + url.toString().replaceFirst( ref + "$", "" );
    }
    /**
     * add the classloader from the given bundle to the LOAD_PATH
     * @param bundle
     */
    public void addBundleToLoadPath(Bundle bundle) {
        addLoadPath(createUri(bundle, "/.jrubydir"));
    }

    /**
     * add the classloader from the given bundle to the LOAD_PATH
     * using the bundle symbolic name
     * 
     * @param symbolicName
     */
    public void addBundleToLoadPath(String symbolicName) {
        addBundleToLoadPath(toBundle(symbolicName));
    }

    /**
     * add the classloader from the given bundle to the GEM_PATH
     * @param bundle
     */
    public void addBundleToGemPath(Bundle bundle) {
        addGemPath(createUri(bundle, "/specifications/.jrubydir"));
    }
 
    /**
     * add the classloader from the given bundle to the GEM_PATH
     * using the bundle symbolic name
     * 
     * @param symbolicName
     */
    public void addBundleToGemPath(String symbolicName) {
        addBundleToGemPath(toBundle(symbolicName));
    }
}
