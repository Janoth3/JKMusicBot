package org.example.Commands;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.managers.AudioManager;
import org.example.LavaPlayer.GuildMusicManager;
import org.example.LavaPlayer.PlayerManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class CommandManager extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if(event.getUser().isBot()){
            return;
        }

        String command = event.getName();

        //Making the bot join a voice channel
        if(command.equals("join")){
            final TextChannel channel = (TextChannel) event.getChannel();
            VoiceChannel connectedChannel = (VoiceChannel) event.getGuild().getSelfMember().getVoiceState().getChannel();

            if(connectedChannel!=null){
                event.reply("I'm already in a voice channel!").queue();
                return;
            }

            final Member member = event.getMember();
            final GuildVoiceState memberVoiceState = member.getVoiceState();

            if(!memberVoiceState.inAudioChannel()){
                event.reply("You need to be in a voice channel for this command to work!").queue();
                return;
            }

            final AudioManager audioManager = event.getGuild().getAudioManager();
            final VoiceChannel memberChannel = (VoiceChannel) memberVoiceState.getChannel();

            audioManager.openAudioConnection(memberChannel);
            event.reply("Connecting to " + memberChannel.getName() + "...").queue();
        }
        //Making the bot leave a voice channel
        else if(command.equals("leave")){
            VoiceChannel connectedChannel = (VoiceChannel) event.getGuild().getSelfMember().getVoiceState().getChannel();

            if(connectedChannel==null){
                event.reply("I'm not in a voice channel!").queue();
                return;
            }

            event.getGuild().getAudioManager().closeAudioConnection();
            event.reply("Disconnected from the voice channel.").queue();
        }
        //Making the bot play a track
        else if(command.equals("play")){
            OptionMapping URLOption = event.getOption("url");
            String URL = URLOption.getAsString();

            if(channelChecks(event)){
                return;
            }

            PlayerManager playerManager = PlayerManager.getInstance();
            playerManager.loadAndPlay((TextChannel) event.getChannel(), URL);
            event.reply("Successfully added song to queue").queue();
        }
        //Making the bot stop playing music
        else if(command.equals("stop")){
            if(channelChecks(event)){
                return;
            }

            final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuild());
            musicManager.scheduler.player.stopTrack();
            musicManager.scheduler.queue.clear();

            event.reply("The player has been stopped and the queue has been cleared.").queue();
        }
        //Making the bot skip a track
        else if(command.equals("skip")){
            if(channelChecks(event)){
                return;
            }

            final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuild());
            final AudioPlayer audioPlayer = musicManager.audioPlayer;

            if(audioPlayer.getPlayingTrack()==null){
                event.reply("There is no track currently playing.").queue();
                return;
            }

            musicManager.scheduler.nextTrack();
            event.reply("The current track has been skipped.").queue();
        }
        //Making the bot show the song currently playing
        else if(command.equals("nowplaying")){
            if(channelChecks(event)){
                return;
            }

            final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuild());
            final AudioPlayer audioPlayer = musicManager.audioPlayer;
            final AudioTrack track = audioPlayer.getPlayingTrack();

            if(track.getInfo()==null){
                event.reply("There is no track currently playing.").queue();
                return;
            }

            final AudioTrackInfo info = track.getInfo();
            event.reply("Now playing " + info.title + " by " + info.author + " (Link: " + info.uri + ")").queue();
            event.reply("e").queue();
        }
        //Making the bot display the current queue of songs
        else if(command.equals("queue")){
            if(channelChecks(event)){
                return;
            }

            final TextChannel channel = (TextChannel) event.getChannel();
            final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuild());
            final BlockingQueue<AudioTrack> queue = musicManager.scheduler.queue;

            if(queue.isEmpty()){
                event.reply("The queue is currently empty").queue();
                return;
            }

            final int trackCount = Math.min(queue.size(), 20);
            final List<AudioTrack> trackList = new ArrayList<>(queue);
            String messageAction = "**Current Queue:**\n";

            for(int i = 0; i < trackCount; i++){
                final AudioTrack track = trackList.get(i);
                final AudioTrackInfo info = track.getInfo();

                messageAction+= i + 1 +" `"+info.title+" by "+info.author+"` [`"+formatTime(track.getDuration())+"`]\n";
            }

            if(trackList.size()>trackCount){
                messageAction+="And `"+ (trackList.size() - trackCount) +"` more...";
            }
            event.reply(messageAction).queue();
        }
        //Making the bot loop the current song
        else if(command.equals("repeat")){
            if(channelChecks(event)){
                return;
            }

            final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuild());
            boolean newRepeating = !musicManager.scheduler.repeating;
            musicManager.scheduler.repeating = newRepeating;
            if(newRepeating){
                event.reply("The player has been set to repeating").queue();
            }
            else if(!newRepeating){
                event.reply("The player has been set to not repeating").queue();
            }
        }
    }
    private String formatTime(long timeInMillis){
        final long hours = timeInMillis/ TimeUnit.HOURS.toMillis(1);
        final long minutes = timeInMillis/TimeUnit.MINUTES.toMillis(1);
        final long seconds = timeInMillis%TimeUnit.MINUTES.toMillis(1)/TimeUnit.SECONDS.toMillis(1);
        return String.format(hours + ":" + minutes + ":" + seconds);
    }

    public boolean channelChecks(SlashCommandInteractionEvent event){
        VoiceChannel connectedChannel = (VoiceChannel) event.getGuild().getSelfMember().getVoiceState().getChannel();

        if(connectedChannel==null){
            event.reply("I need to be in a voice channel for this to work").queue();
            return true;
        }

        final Member member = event.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();

        if(connectedChannel!=(VoiceChannel) memberVoiceState.getChannel()){
            event.reply("You need to be in the same channel as me for this to work.").queue();
            return true;
        }
        return false;
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        List<CommandData> commandData = new ArrayList<>();

        commandData.add(Commands.slash("join","Make the bot join your voice channel."));
        commandData.add(Commands.slash("leave","Make the bot leave your voice channel."));

        OptionData trackURL = new OptionData(OptionType.STRING, "url", "The YouTube URL you want the bot to play", true);
        commandData.add(Commands.slash("play", "Make the bot play a track.").addOptions(trackURL));

        commandData.add(Commands.slash("stop", "Make the bot stop playing music"));
        commandData.add(Commands.slash("skip", "Make the bot skip the current track"));
        commandData.add(Commands.slash("nowplaying", "Shows the song currently playing"));
        commandData.add(Commands.slash("queue", "Shows current queue of songs (up to 20)"));
        commandData.add(Commands.slash("repeat", "Loop the current song"));

        event.getGuild().updateCommands().addCommands(commandData).queue();
    }
}
