package at.hannibal2.skyhanni.sign;

import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.tileentity.TileEntitySign;

import java.lang.reflect.Field;

public class LorenzSignUtils {

    private static boolean once = false;
    private static Field field = null;
    //(field_146848_f) class net.minecraft.client.gui.inventory.GuiEditSign

    public static TileEntitySign getTileSign(GuiEditSign editSign) {

//        Field field1 = ReflectionHelper.findField(editSign.getClass(), "editSign");
//        ObfuscationReflectionHelper.getPrivateValue(editSign.getClass(), editSign, 0)

        if (field != null) {
            try {
                return (TileEntitySign) field.get(editSign);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("hidden", e);
            }
        }

        if (once) {
            throw new RuntimeException("hidden");
        }
        once = true;

        try {
            Class<? extends GuiEditSign> aClass = editSign.getClass();
//            System.out.println("");
//            System.out.println("");
//            System.out.println("");
            for (Field field : aClass.getDeclaredFields()) {
                String name = field.getName();
                Class<?> fieldDeclaringClass = field.getDeclaringClass();
//                System.out.println("");
//                System.out.println("(" + name + ") " + fieldDeclaringClass);

                field.setAccessible(true);
                Object o = field.get(editSign);
                if (o instanceof TileEntitySign) {
//                    System.out.println("DONE!!!!!");
                    LorenzSignUtils.field = field;
                    return (TileEntitySign) o;
                }

//                System.out.println("");
            }

//            System.out.println("");
//            System.out.println("");
//            System.out.println("");
            Field field = aClass.getDeclaredField("tileSign");
            field.setAccessible(true);
            return (TileEntitySign) field.get(editSign);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("can not refactor getTileSign! (" + e.getMessage() + ")", e);
        }
    }
}
