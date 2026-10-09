package com.cwnu.kitchenmood.model;

import java.util.Locale;

public enum Mood {
    CALM(
            "차분하게", "☾",
            "천천히 끓는 냄비처럼, 마음에도 여유를 주세요.",
            "느긋한 오후의 식탁",
            0xFF15221F, 0xFFD3BA86, 0xFF21342D, 0xFFDCE5D5,
            new String[]{
                    "잘 익는 데에는, 조금의 기다림이 필요해요.",
                    "오늘의 한 끼는 천천히 만들어도 괜찮아요.",
                    "냄비가 끓는 동안, 어깨의 힘도 내려놓아요.",
                    "작은 한 숟가락이 하루를 따뜻하게 바꿔요.",
                    "완벽한 요리보다 편안한 식탁을 준비해요.",
                    "불을 한 칸 낮추고, 숨을 한 번 길게 쉬어요.",
                    "따뜻한 향기가 부엌에 머무는 순간을 즐겨요."
            }),
    GROOVE(
            "신나게", "✦",
            "가벼운 리듬을 더하면, 익숙한 요리도 새로워져요.",
            "오렌지빛 주방 리듬",
            0xFF29201E, 0xFFEDA87C, 0xFF45322B, 0xFFF1D7BC,
            new String[]{
                    "한 꼬집의 즐거움도 오늘의 레시피에 넣어요.",
                    "프라이팬 위에서 오늘의 좋은 기분이 익어요.",
                    "즐겁게 만든 한 끼는 식탁에서도 빛나요.",
                    "재료를 꺼내며 좋아하는 리듬을 떠올려요.",
                    "평범한 저녁에 상큼한 한 가지를 더해봐요.",
                    "오늘의 메뉴 이름은 기분 좋은 한 접시예요.",
                    "맛있는 향기가 번질수록 마음도 가벼워져요."
            }),
    FOCUS(
            "집중해서", "◎",
            "작은 소리에 기대어, 눈앞의 한 단계에 집중해요.",
            "초록빛 레시피 노트",
            0xFF1B2130, 0xFFAABDE1, 0xFF2D3649, 0xFFD8DEED,
            new String[]{
                    "다음 한 단계만 천천히 살펴봐요.",
                    "재료를 정리하면 마음에도 공간이 생겨요.",
                    "지금 필요한 만큼만, 차근차근 준비해요.",
                    "한 가지씩 해내면 어느새 한 끼가 완성돼요.",
                    "손끝의 작은 변화에 집중해보는 시간이에요.",
                    "레시피의 한 줄, 오늘의 작은 성취예요.",
                    "완성까지 남은 과정도 편안하게 이어가요."
            });

    public final String label;
    public final String icon;
    public final String description;
    public final String stationTitle;
    public final int backgroundColor;
    public final int accentColor;
    public final int cardColor;
    public final int noteColor;
    public final String[] notes;

    Mood(String label, String icon, String description, String stationTitle,
         int backgroundColor, int accentColor, int cardColor, int noteColor, String[] notes) {
        this.label = label;
        this.icon = icon;
        this.description = description;
        this.stationTitle = stationTitle;
        this.backgroundColor = backgroundColor;
        this.accentColor = accentColor;
        this.cardColor = cardColor;
        this.noteColor = noteColor;
        this.notes = notes;
    }

    public static Mood fromName(String name) {
        if (name == null) return CALM;
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return CALM;
        }
    }
}
