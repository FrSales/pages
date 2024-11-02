import java.util.*;

public class Main {
    public static void main(String[] args) {
        int frame_count = 3;
        int[] pages = {6, 6, 1, 6, 2, 10, 8, 3, 10, 8, 7, 5, 6, 2, 3, 10, 2, 6, 9, 4};

        PageReplacement simulator = new PageReplacement(frame_count);

        System.out.println("FIFO Simulation:");
        int fifo_fault = simulator.simulateFIFO(pages);
        System.out.println("Total Page Faults (FIFO): " + fifo_fault);

        System.out.println("\nLRU Simulation:");
        int lru_fault = simulator.simulateLRU(pages);
        System.out.println("Total Page Faults (LRU): " + lru_fault);

        System.out.println("\nClock Simulation:");
        int clock_fault = simulator.simulateClock(pages);
        System.out.println("Total Page Faults (Clock): " + clock_fault);

        System.out.println("\nNFU Simulation:");
        int nfu_fault = simulator.simulateNFU(pages);
        System.out.println("Total Page Faults (NFU): " + nfu_fault);

        System.out.println("\nAging Simulation:");
        int aging_fault = simulator.simulateAging(pages);
        System.out.println("Total Page Faults (Aging): " + aging_fault);

        System.out.println("\nOptimal Simulation:");
        int optimal_fault = simulator.simulateOptimal(pages);
        System.out.println("Total Page Faults (Optimal): " + optimal_fault);
    }
}

class PageReplacement {
    private final int frame_count;
    private int page_faults;
    private final Queue<Integer> fifo_frames;
    private final LinkedHashMap<Integer, Integer> lru_frames;
    private List<Integer> clock_frames;
    private final Map<Integer, Boolean> reference_bits;
    private int clock_pointer;
    private final Map<Integer, Integer> nfu_frequency;
    private final Map<Integer, Integer> agingCounters;

    public PageReplacement(int frame_count) {
        this.frame_count = frame_count;
        this.fifo_frames = new LinkedList<>();
        this.lru_frames = new LinkedHashMap<>(frame_count, 0.75f, true);
        this.clock_frames = new ArrayList<>(Collections.nCopies(frame_count, -1));
        this.reference_bits = new HashMap<>();
        this.clock_pointer = 0;
        this.nfu_frequency = new HashMap<>();
        this.agingCounters = new HashMap<>();
        this.page_faults = 0;
    }
    
    private void reset() {
        page_faults = 0;
        fifo_frames.clear();
        lru_frames.clear();
        clock_frames = new ArrayList<>(Collections.nCopies(frame_count, -1));
        clock_pointer = 0;
        reference_bits.clear();
        nfu_frequency.clear();
        agingCounters.clear();
    }
    
    public int simulateFIFO(int[] pages) {
        this.reset();

        for (int page : pages) {
            if (!fifo_frames.contains(page)) {
                page_faults++;
                if (fifo_frames.size() == frame_count) {
                    fifo_frames.poll();
                }
                fifo_frames.add(page);
            }
            System.out.println("FIFO Frames: " + fifo_frames);
        }
        return page_faults; 
    }

    public int simulateLRU(int[] pages) {
        this.reset();

        for (int page : pages) {
            if (!lru_frames.containsKey(page)) {
                page_faults++;
                if (lru_frames.size() == frame_count) {
                    int lruPage = lru_frames.keySet().iterator().next();
                    lru_frames.remove(lruPage);
                }
            }
            lru_frames.put(page, page);
            System.out.println("LRU Frames: " + lru_frames.keySet());
        }
        return page_faults; 
    }

    public int simulateClock(int[] pages) {
        this.reset();

        for (int page : pages) {
            if (!clock_frames.contains(page)) {
                page_faults++;
                while (true) {
                    int currentPage = clock_frames.get(clock_pointer);
                    if (currentPage == -1 || !reference_bits.getOrDefault(currentPage, false)) {
                        clock_frames.set(clock_pointer, page);
                        reference_bits.put(page, true);
                        clock_pointer = (clock_pointer + 1) % frame_count;
                        break;
                    } else {
                        reference_bits.put(currentPage, false);
                        clock_pointer = (clock_pointer + 1) % frame_count;
                    }
                }
            } else {
                reference_bits.put(page, true);
            }
            System.out.println("Clock Frames: " + clock_frames);
        }
        return page_faults; 
    }

    public int simulateOptimal(int[] pages) {
        this.reset();
        List<Integer> optimal_frames = new ArrayList<>(Collections.nCopies(frame_count, -1));

        for (int i = 0; i < pages.length; i++) {
            int page = pages[i];

            if (!optimal_frames.contains(page)) {
                page_faults++;
                if (optimal_frames.contains(-1)) {
                    optimal_frames.set(optimal_frames.indexOf(-1), page);
                } else {
                    int furthestIndex = -1;
                    int pageToReplace = -1;

                    for (int frame_page : optimal_frames) {
                        int nextUse = Integer.MAX_VALUE;
                        for (int j = i + 1; j < pages.length; j++) {
                            if (pages[j] == frame_page) {
                                nextUse = j;
                                break;
                            }
                        }

                        if (nextUse == Integer.MAX_VALUE) {
                            pageToReplace = frame_page;
                            break;
                        } else if (nextUse > furthestIndex) {
                            furthestIndex = nextUse;
                            pageToReplace = frame_page;
                        }
                    }
                    optimal_frames.set(optimal_frames.indexOf(pageToReplace), page);
                }
            }
            System.out.println("Optimal Frames: " + optimal_frames);
        }
        return page_faults; 
    }

    public int simulateNFU(int[] pages) {
        this.reset();
        List<Integer> nfuFrames = new ArrayList<>(Collections.nCopies(frame_count, -1));

        for (int page : pages) {
            nfu_frequency.put(page, nfu_frequency.getOrDefault(page, 0) + 1);

            if (!nfuFrames.contains(page)) {
                page_faults++;
                if (nfuFrames.contains(-1)) {
                    int emptySlot = nfuFrames.indexOf(-1);
                    nfuFrames.set(emptySlot, page);
                } else {
                    int leastUsedPage = nfuFrames.get(0);
                    for (int p : nfuFrames) {
                        if (nfu_frequency.get(p) < nfu_frequency.get(leastUsedPage)) {
                            leastUsedPage = p;
                        }
                    }
                    nfuFrames.set(nfuFrames.indexOf(leastUsedPage), page);
                    nfu_frequency.remove(leastUsedPage);
                }
            }
            System.out.println("NFU Frames: " + nfuFrames);
        }
        return page_faults; 
    }

    public int simulateAging(int[] pages) {
        this.reset();
        List<Integer> agingFrames = new ArrayList<>(Collections.nCopies(frame_count, -1));

        for (int page : pages) {
            agingCounters.replaceAll((k, v) -> agingCounters.get(k) >> 1);

            if (!agingFrames.contains(page)) {
                page_faults++;
                if (agingFrames.contains(-1)) {
                    int emptySlot = agingFrames.indexOf(-1);
                    agingFrames.set(emptySlot, page);
                    agingCounters.put(page, 1 << 7);
                } else {
                    int oldestPage = agingFrames.get(0);
                    for (int p : agingFrames) {
                        if (agingCounters.getOrDefault(p, Integer.MAX_VALUE) < agingCounters.get(oldestPage)) {
                            oldestPage = p;
                        }
                    }
                    agingFrames.set(agingFrames.indexOf(oldestPage), page);
                    agingCounters.remove(oldestPage);
                    agingCounters.put(page, 1 << 7);
                }
            } else {
                agingCounters.put(page, (agingCounters.get(page) >> 1) | (1 << 7));
            }
            System.out.println("Aging Frames: " + agingFrames + ", Counters: " + agingCounters);
        }
        return page_faults; 
    }
}
