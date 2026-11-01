package thaumcraft.port.research;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

/** The three original dream pages, localized by the reader's client. */
public final class DreamJournal {
    public static ItemStack create(String author){
        var book=new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT,new WrittenBookContent(
                Filterable.passThrough("Strange Dreams"),author,3,
                List.of(page(1),page(2),page(3)),true));
        book.set(DataComponents.ITEM_NAME,Component.translatable("book.start.title"));
        return book;
    }
    private static Filterable<Component> page(int number){return Filterable.passThrough(Component.translatable("book.start."+number));}
    private DreamJournal(){}
}
