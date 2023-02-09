package org.example.LavaPlayer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TrackScheduler extends AudioEventAdapter {
    public AudioPlayer player;
    public BlockingQueue<AudioTrack> queue;
    public boolean repeating = false;

    public TrackScheduler(AudioPlayer player){
        this.player = player;
        this.queue = new LinkedBlockingQueue<>();
    }
    public void queue(AudioTrack track){
        if(!this.player.startTrack(track,true)){
            this.queue.offer(track);
        }
    }
    public void nextTrack(){
        this.player.startTrack(this.queue.poll(),false);
    }

    public void repeatTrack(AudioTrack track){
        BlockingQueue<AudioTrack> newQueue = new LinkedBlockingQueue<>();
        newQueue.add(track.makeClone());
        int trackCount = Math.min(queue.size(), 20);
        List<AudioTrack> trackList = new ArrayList<>(queue);
        for(int i = 0; i<trackCount; i++){
            AudioTrack thisTrack = trackList.get(i);
            newQueue.add(thisTrack);
        }
        this.queue = newQueue;
    }
    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if(endReason.mayStartNext){
            if(this.repeating){
                repeatTrack(track);
            }
            nextTrack();
        }
    }
}
