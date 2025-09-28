package org.king_shack.duck;

/** SubtitleEntry에 주입해서 생성시간을 보관하기 위한 인터페이스 */
public interface KSEntryExt {
    long ks$getStartAt();
    void ks$setStartAt(long timeMillis);
}
