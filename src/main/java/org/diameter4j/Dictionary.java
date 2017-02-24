package org.diameter4j;

import org.diameter4j.base.Common;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class Dictionary {

    private static Dictionary instance = new Dictionary();

    static {
        instance.load(Common.class);
    }

    public static Dictionary getInstance() {
        return instance;
    }

    private Map<Long, Type<?>> types = new HashMap<>();
    private Map<Integer, Command> requests = new HashMap<Integer, Command>();
    private Map<Integer, Command> answers = new HashMap<Integer, Command>();

    public Type<?> getType(int vendorId, int code) {
        return types.get(id(vendorId, code));
    }

    public void load(Class<?> clazz)
    {
        Field[] fields = clazz.getDeclaredFields();
        for (int i = 0; i < fields.length; i++)
        {
            Field field = fields[i];
            if (((field.getModifiers() & Modifier.STATIC) == Modifier.STATIC))
            {
                try
                {
                    if (Type.class.isAssignableFrom(field.getType()))
                    {

                        Type<?> type = (Type<?>) field.get(null);
                        types.put(id(type.getVendorId(), type.getCode()), type);
                    }
                    else if (Command.class.isAssignableFrom(field.getType()))
                    {
                        Command command = (Command) field.get(null);
                        if (command.isRequest())
                            requests.put(command.getCode(), command);
                        else
                            answers.put(command.getCode(), command);
                    }
                } catch (Exception e)  {
                    e.printStackTrace();
                }
            }
        }
    }

    public long id(int vendorId, int code) {
        return (long) vendorId << 32 | code;
    }
}
