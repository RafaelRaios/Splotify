import javazoom.jl.decoder.*;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;
import support.PlayerWindow;
import support.Song;

import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.ArrayList;

public class Player {

    /**
     * The MPEG audio bitstream.
     */
    private Bitstream bitstream;
    /**
     * The MPEG audio decoder.
     */
    private Decoder decoder;
    /**
     * The AudioDevice where audio samples are written to.
     */
    private AudioDevice device;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition lockCondition = lock.newCondition();
    private PlayerWindow window;
    private ArrayList<String[]> musicsListDynamic = new ArrayList<String[]>();
    private String[][] staticmusicList = {};
    private ArrayList<Song> songListDynamic = new ArrayList();
    private ArrayList<Song> backupSongListDynamic = new ArrayList();
    private Song currentSong;

    private boolean isLooping = false;
    private boolean isPlaying = false;
    private boolean moreThamOne = false;
    private  boolean isRunning = false;
    private boolean isShuffled = false;

    private int musicIndex;
    private int playIndex;
    private int actualPlayPause;
    private int listSize = 0;
    private int currentFrame = 0;
    private int pressButtonPlayPause = 0;

    //funções
    private final ActionListener buttonListenerPlayNow = e -> {
        pressButtonPlayPause = 1;
        playNow(0);
    };
    private final ActionListener buttonListenerRemove = e -> {
        Thread removesong = new Thread(() -> {
            try {
                lock.lock();
                musicIndex = window.getSelectedSongIndex();
                listSize--;
                String[] RemovMusic = musicsListDynamic.get(musicIndex);
                musicsListDynamic.remove(RemovMusic);
                Song aux = songListDynamic.get(musicIndex);
                songListDynamic.remove(aux);
                backupSongListDynamic.remove(aux);
                staticmusicList = musicsListDynamic.toArray(new String[this.musicsListDynamic.size()][7]);
                window.setQueueList(staticmusicList);
                if(playIndex == musicIndex) {


                    if(playIndex < listSize) {
                        EventQueue.invokeLater(() -> {
                            moreThamOne = true;
                            playNow(2);
                        });
                    }
                    else {
                        EventQueue.invokeLater(() -> {
                            pare();
                        });
                    }
                }
                if (listSize < 2) {
                    EventQueue.invokeLater(() -> {
                        window.setEnabledShuffleButton(false);
                    });
                } else {
                    EventQueue.invokeLater(() -> {
                        window.setEnabledShuffleButton(true);
                    });
                }
                if (listSize >= 1) {
                    EventQueue.invokeLater(() -> {
                        window.setEnabledLoopButton(true);
                    });
                } else {
                    EventQueue.invokeLater(() -> {
                        window.setEnabledLoopButton(false);
                    });
                }


            } finally {
                lock.unlock();
            }
        });
        removesong.start();;
    };
    private final ActionListener buttonListenerAddSong = e -> {
        Thread addsong = new Thread(() -> {
            try {
                lock.lock();
                Song music = window.openFileChooser();
                musicsListDynamic.add(music.getDisplayInfo());
                staticmusicList = musicsListDynamic.toArray(new String[this.musicsListDynamic.size()][7]);
                window.setQueueList(staticmusicList);
                songListDynamic.add(music);
                backupSongListDynamic.add(music);
                listSize++;

                if (listSize < 2) {
                    EventQueue.invokeLater(() -> {
                        window.setEnabledShuffleButton(false);
                    });
                } else {
                    EventQueue.invokeLater(() -> {
                        window.setEnabledShuffleButton(true);
                    });
                }
                if (listSize >= 1) {
                    EventQueue.invokeLater(() -> {
                        window.setEnabledLoopButton(true);
                    });
                } else {
                    EventQueue.invokeLater(() -> {
                        window.setEnabledLoopButton(false);
                    });
                }

            }  finally {
                lock.unlock();
            }
        });
        addsong.start();
    };
    private final ActionListener buttonListenerPlayPause = e -> {
        if (pressButtonPlayPause == 1){
            EventQueue.invokeLater(() -> {
                window.setPlayPauseButtonIcon(0);
            });

            pressButtonPlayPause = 0;
            actualPlayPause = 0;

        } else{
            actualPlayPause = 1;
            pressButtonPlayPause = 1;
            playNow(1);

            EventQueue.invokeLater(() -> {
                window.setPlayPauseButtonIcon(1);
            });
        }

    };
    private final ActionListener buttonListenerStop = e -> {
        pare();
    };
    private final ActionListener buttonListenerNext = e -> {
        playNextSong();
    };
    private final ActionListener buttonListenerPrevious = e -> {
        playPreviousSong();
    };
    private final ActionListener buttonListenerShuffle = e -> {
        Thread shuffle = new Thread(() -> {
            try {
                lock.lock();
                if (!isShuffled) {
                    isShuffled = true;
                    backupSongListDynamic = (ArrayList<Song>) songListDynamic.clone();
                    currentSong = backupSongListDynamic.get(playIndex);
                    Collections.shuffle(songListDynamic);

                    if(isPlaying) {
                        songListDynamic.remove(currentSong);
                        songListDynamic.add(0, currentSong);
                    }
                    playIndex = 0;

                } else {
                    isShuffled = false;
                    currentSong = songListDynamic.get(playIndex);
                    songListDynamic = backupSongListDynamic;
                    playIndex = songListDynamic.indexOf(currentSong);
                }

                musicsListDynamic.clear();
                for (Song i: songListDynamic){
                    musicsListDynamic.add(i.getDisplayInfo());
                }

                staticmusicList = musicsListDynamic.toArray(new String[this.musicsListDynamic.size()][7]);
                window.setQueueList(staticmusicList);

            }
            finally {
                lock.unlock();
            }
        });

        shuffle.start();
    };
    private final ActionListener buttonListenerLoop = e -> {
        isLooping = !isLooping;
    };
    private final MouseInputAdapter scrubberMouseInputAdapter = new MouseInputAdapter() {
        @Override
        public void mouseReleased(MouseEvent e) {
            scrubberAlter();
            if (actualPlayPause == 1) pressButtonPlayPause = 1;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            momentumPouse();
            updateTime();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            updateTime();
        }
    };

    public Player() {
        String[][] queue = {};
        EventQueue.invokeLater(() -> window = new PlayerWindow(
                ("Splotify"),
                queue,
                buttonListenerPlayNow,
                buttonListenerRemove,
                buttonListenerAddSong,
                buttonListenerShuffle,
                buttonListenerPrevious,
                buttonListenerPlayPause,
                buttonListenerStop,
                buttonListenerNext,
                buttonListenerLoop,
                scrubberMouseInputAdapter)
        );
    }

    //<editor-fold desc="Essential">
    /**
     * @return False if there are no more frames to play.
     */
    private boolean playNextFrame() throws JavaLayerException {
        if (device != null) {
            Header h = bitstream.readFrame();
            if (h == null) return false;

            SampleBuffer output = (SampleBuffer) decoder.decodeFrame(h, bitstream);
            device.write(output.getBuffer(), 0, output.getBufferLength());
            bitstream.closeFrame();
            currentFrame++;
        }
        return true;
    }

    /**
     * @return False if there are no more frames to skip.
     */
    private boolean skipNextFrame() throws BitstreamException {
        Header h = bitstream.readFrame();
        if (h == null) return false;
        bitstream.closeFrame();
        currentFrame++;
        return true;
    }

    /**
     * Skips bitstream to the target frame if the new frame is higher than the current one.
     *
     * @param newFrame Frame to skip to.
     */
    private void skipToFrame(int newFrame) throws BitstreamException {
        int framesToSkip = newFrame - currentFrame;
        boolean condition = true;
        while (framesToSkip-- > 0 && condition) condition = skipNextFrame();
    }
    //</editor-fold>
    private void playNow(int repeat) { //repeat = 0 inicio, 1 = pause

        Thread Play = new Thread(() -> {
            if(isPlaying) {
                moreThamOne = true;
            }
            if(repeat % 2 == 0) {
                actualPlayPause = 1;
                currentFrame = 0;
                EventQueue.invokeLater(() -> {
                    window.setPlayPauseButtonIcon(1);
                });

                if(repeat == 0) {
                    musicIndex = window.getSelectedSongIndex();
                    playIndex = musicIndex;
                }

                if(repeat == 2) pressButtonPlayPause = 1;
                currentSong = songListDynamic.get(playIndex);
                setup();
                isPlaying = true;
            }
            EventQueue.invokeLater(() -> {
                window.setPlayingSongInfo(currentSong.getTitle(), currentSong.getAlbum(), currentSong.getArtist());
            });
            while (isPlaying) {
                if(pressButtonPlayPause == 1) {
                    try {
                        if(moreThamOne) {
                            moreThamOne = false;
                            break;
                        }
                        EventQueue.invokeLater(() -> {
                            window.setTime((currentFrame * (int) currentSong.getMsPerFrame()), (int) currentSong.getMsLength());
                            window.setEnabledPlayPauseButton(isPlaying);
                            window.setEnabledStopButton(isPlaying);
                            window.setEnabledScrubber(isPlaying);
                        });
                        if (!(listSize == 1 || playIndex >= listSize - 1)) {
                            EventQueue.invokeLater(() -> {
                                window.setEnabledNextButton(isPlaying);
                            });
                        } else {
                            EventQueue.invokeLater(() -> {
                                window.setEnabledNextButton(false);
                            });
                        }
                        if(!(listSize == 1 || playIndex == 0)) {
                            EventQueue.invokeLater(() -> {
                                window.setEnabledPreviousButton(isPlaying);
                            });
                        } else {
                            EventQueue.invokeLater(() -> {
                                window.setEnabledPreviousButton(false);
                            });
                        }
                        isRunning = playNextFrame();
                        if(isRunning == false) {
                            if((playIndex < listSize - 1)) {
                                playIndex++;
                                EventQueue.invokeLater(() -> {
                                    pare();
                                });

                                playNow(2);
                            }
                            else if(isLooping && playIndex == listSize - 1) {
                                playIndex = 0;
                                EventQueue.invokeLater(() -> {
                                    pare();
                                });

                                playNow(2);
                            }
                            else
                            {
                                EventQueue.invokeLater(() -> {
                                    pare();
                                });
                                isPlaying = false;;
                            }

                        }

                    } catch (JavaLayerException e) {
                        throw new RuntimeException(e);
                    }
                }
                System.out.println();
            }
        });
        Play.start();
    }
    private void setup() {
        try {
            device = FactoryRegistry.systemRegistry().createAudioDevice();
            device.open(decoder = new Decoder());
            bitstream = new Bitstream(currentSong.getBufferedInputStream());

        } catch (JavaLayerException | FileNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }
    private void pare(){
        isPlaying = false;
        window.resetMiniPlayer();
    }
    private void scrubberAlter() {
        try {

            currentFrame = 0;
            device = FactoryRegistry.systemRegistry().createAudioDevice();
            device.open(decoder = new Decoder());
            bitstream = new Bitstream(currentSong.getBufferedInputStream());

        } catch (JavaLayerException | FileNotFoundException ex) {
            throw new RuntimeException(ex);
        }

        int newTime = (int) (window.getScrubberValue()/currentSong.getMsPerFrame());
        window.setTime(((int) currentSong.getMsPerFrame() * newTime), (int) currentSong.getMsLength());

        try {
            skipToFrame(newTime);
        } catch (BitstreamException e) {
            throw new RuntimeException(e);
        }
    }
    private void momentumPouse() {

        pressButtonPlayPause = 0;

    }
    private void playPreviousSong() {
        playIndex--;
        moreThamOne = true;
        playNow(2);

    }
    private void playNextSong() {
        playIndex++;
        moreThamOne = true;
        playNow(2);


    }
    private void updateTime() {
        try {
            lock.lock();
            EventQueue.invokeLater(() -> {
                int newTime = (int) (window.getScrubberValue()/currentSong.getMsPerFrame());
                window.setTime(((int) currentSong.getMsPerFrame() * newTime), (int) currentSong.getMsLength());
            });
        }
        finally {
            lock.unlock();
        }
    }


}