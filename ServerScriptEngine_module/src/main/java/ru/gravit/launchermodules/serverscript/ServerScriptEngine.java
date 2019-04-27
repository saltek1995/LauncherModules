package ru.gravit.launchermodules.serverscript;

import ru.gravit.launchserver.LaunchServer;
import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.IOHelper;
import ru.gravit.utils.helper.JarHelper;
import ru.gravit.utils.helper.LogHelper;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

public class ServerScriptEngine {
    public final ScriptEngine engine = CommonHelper.newScriptEngine();
    public Map<String, String> mappings;

    public Object loadScript(String path) throws IOException, ScriptException {
        URL url = IOHelper.getResourceURL(path);
        LogHelper.debug("Loading script: '%s'", url);
        try (BufferedReader reader = IOHelper.newReader(url)) {
            return engine.eval(reader, engine.getBindings(ScriptContext.ENGINE_SCOPE));
        }
    }
    public Object eval(String line) throws ScriptException
    {
        return engine.eval(line, engine.getBindings(ScriptContext.ENGINE_SCOPE));
    }
    public void initBaseBindings()
    {
        setScriptBindings();
    }
    private void setScriptBindings() {
        LogHelper.info("Setting up script engine bindings");
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put("engine", this);
        bindings.put("launchserver", LaunchServer.server);

        try {
            mappings = JarHelper.jarMap(LaunchServer.class, false);
            for(Map.Entry<String, String> e : mappings.entrySet())
            {
                if(!e.getValue().startsWith("ru.gravit")) continue; //Отсекаем библиотеки
                try {
                    Class<?> clazz = Class.forName(e.getValue(), false, ClassLoader.getSystemClassLoader());
                    String bindClassName = e.getKey() + "Class";
                    bindings.put(bindClassName, clazz);
                    eval(String.format("var %s = %s.static;", e.getKey(), bindClassName));
                } catch (ClassNotFoundException ex)
                {
                    LogHelper.debug("[ScriptEngine] Class %s not found", e.getValue());
                } catch (ScriptException e1) {
                    LogHelper.error(e1);
                }
            }
        } catch (IOException e) {
            LogHelper.error(e);
        }
    }
}