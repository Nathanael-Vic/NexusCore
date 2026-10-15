package com.nexussmp.nexuscore.core.command;

import com.nexussmp.nexuscore.backrooms.command.BackroomsCommands;
import com.nexussmp.nexuscore.display.command.PanelCommands;
import com.nexussmp.nexuscore.hologram.command.HologramCommands;
import com.nexussmp.nexuscore.magic.command.MagicCommands;
import com.nexussmp.nexuscore.parrot.command.ParrotCommands;
import com.nexussmp.nexuscore.quest.command.QuestCommands;
import com.nexussmp.nexuscore.radio.command.RadioCommands;
import com.nexussmp.nexuscore.stats.command.StatsCommands;
import com.nexussmp.nexuscore.vampire.command.VampireCommands;
import net.minecraft.commands.Commands;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Raiz {@code /nexus}. Cada modulo contribui com o seu ramo. */
public final class NexusCommands {

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("nexus")
                .then(StatsCommands.build())
                .then(PanelCommands.build())
                .then(HologramCommands.build())
                .then(QuestCommands.build())
                .then(BackroomsCommands.build())
                .then(RadioCommands.build())
                .then(VampireCommands.build())
                .then(MagicCommands.build())
                .then(ParrotCommands.build()));
    }

    private NexusCommands() {}
}
