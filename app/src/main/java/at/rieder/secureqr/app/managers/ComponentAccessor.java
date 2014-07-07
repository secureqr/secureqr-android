package at.rieder.secureqr.app.managers;

import java.util.HashMap;
import java.util.Map;

import at.rieder.secureqr.app.camera.CameraManager;

/**
 * Created by Thomas on 15.03.14.
 */
public final class ComponentAccessor {

    private static ComponentAccessor componentAccessor = null;

    private Map<Class, Object> componentMap;

    private ComponentAccessor() {
        this.componentMap = new HashMap<Class, Object>();
    }


    public synchronized static ComponentAccessor getInstance() {
        if (componentAccessor == null) {
            componentAccessor = new ComponentAccessor();
        }
        return componentAccessor;
    }

    public void addComponent(Object object) {
        this.componentAccessor.componentMap.put(object.getClass(), object);
    }

    public CameraManager getCameraManager() {
        if (this.componentAccessor.componentMap.containsKey(CameraManager.class)) {
            Object o = this.componentAccessor.componentMap.get(CameraManager.class);
            if (o instanceof CameraManager) {
                return (CameraManager) o;
            }
        }
        return null;
    }

    public HistoryManager getHistoryManager() {
        if (this.componentAccessor.componentMap.containsKey(HistoryManager.class)) {
            Object o = this.componentAccessor.componentMap.get(HistoryManager.class);
            if (o instanceof HistoryManager) {
                return (HistoryManager) o;
            }
        }
        return null;
    }

    private Map<Class, Object> getComponentMap() {
        return componentMap;
    }

}
