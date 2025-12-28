package github.dimazbtw.dungeons.models;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sistema de agendamento de dungeons
 * Permite definir horários específicos em que a dungeon está disponível
 */
public class DungeonSchedule {

    private boolean enabled;
    private List<TimePeriod> periods;
    private Set<DayOfWeek> allowedDays;
    private String closedMessage;

    public DungeonSchedule() {
        this.enabled = false;
        this.periods = new ArrayList<>();
        this.allowedDays = new HashSet<>();
        // Por padrão, todos os dias
        for (DayOfWeek day : DayOfWeek.values()) {
            allowedDays.add(day);
        }
        this.closedMessage = "&cEsta dungeon está fechada no momento.";
    }

    /**
     * Verifica se a dungeon está aberta agora
     */
    public boolean isOpen() {
        if (!enabled) return true; // Se não tem schedule, sempre aberta

        LocalDateTime now = LocalDateTime.now();
        
        // Verificar dia da semana
        if (!allowedDays.contains(now.getDayOfWeek())) {
            return false;
        }

        // Verificar horário
        LocalTime currentTime = now.toLocalTime();
        for (TimePeriod period : periods) {
            if (period.contains(currentTime)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Retorna o próximo horário de abertura
     */
    public String getNextOpenTime() {
        if (!enabled || periods.isEmpty()) return "Sempre aberta";

        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();
        DayOfWeek currentDay = now.getDayOfWeek();

        // Verificar se abre hoje ainda
        if (allowedDays.contains(currentDay)) {
            for (TimePeriod period : periods) {
                if (currentTime.isBefore(period.getStart())) {
                    return "Hoje às " + period.getStartFormatted();
                }
            }
        }

        // Procurar próximo dia
        for (int i = 1; i <= 7; i++) {
            DayOfWeek checkDay = currentDay.plus(i);
            if (allowedDays.contains(checkDay)) {
                if (!periods.isEmpty()) {
                    String dayName = getDayName(checkDay);
                    return dayName + " às " + periods.get(0).getStartFormatted();
                }
            }
        }

        return "Indisponível";
    }

    /**
     * Retorna string formatada dos horários
     */
    public String getScheduleString() {
        if (!enabled) return "&aSempre aberta";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < periods.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(periods.get(i).toString());
        }

        if (allowedDays.size() < 7) {
            sb.append(" (");
            List<String> dayNames = new ArrayList<>();
            for (DayOfWeek day : allowedDays) {
                dayNames.add(getDayNameShort(day));
            }
            sb.append(String.join(", ", dayNames));
            sb.append(")");
        }

        return sb.toString();
    }

    private String getDayName(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "Segunda";
            case TUESDAY -> "Terça";
            case WEDNESDAY -> "Quarta";
            case THURSDAY -> "Quinta";
            case FRIDAY -> "Sexta";
            case SATURDAY -> "Sábado";
            case SUNDAY -> "Domingo";
        };
    }

    private String getDayNameShort(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "Seg";
            case TUESDAY -> "Ter";
            case WEDNESDAY -> "Qua";
            case THURSDAY -> "Qui";
            case FRIDAY -> "Sex";
            case SATURDAY -> "Sáb";
            case SUNDAY -> "Dom";
        };
    }

    // Getters e Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public List<TimePeriod> getPeriods() { return periods; }
    public void setPeriods(List<TimePeriod> periods) { this.periods = periods; }
    public void addPeriod(TimePeriod period) { this.periods.add(period); }

    public Set<DayOfWeek> getAllowedDays() { return allowedDays; }
    public void setAllowedDays(Set<DayOfWeek> allowedDays) { this.allowedDays = allowedDays; }

    public String getClosedMessage() { return closedMessage; }
    public void setClosedMessage(String closedMessage) { this.closedMessage = closedMessage; }

    /**
     * Representa um período de tempo (início -> fim)
     */
    public static class TimePeriod {
        private LocalTime start;
        private LocalTime end;
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

        public TimePeriod(String start, String end) {
            this.start = LocalTime.parse(start, FORMATTER);
            this.end = LocalTime.parse(end, FORMATTER);
        }

        public TimePeriod(LocalTime start, LocalTime end) {
            this.start = start;
            this.end = end;
        }

        public boolean contains(LocalTime time) {
            // Lidar com períodos que cruzam meia-noite
            if (end.isBefore(start)) {
                return !time.isBefore(start) || !time.isAfter(end);
            }
            return !time.isBefore(start) && !time.isAfter(end);
        }

        public LocalTime getStart() { return start; }
        public LocalTime getEnd() { return end; }

        public String getStartFormatted() { return start.format(FORMATTER); }
        public String getEndFormatted() { return end.format(FORMATTER); }

        @Override
        public String toString() {
            return start.format(FORMATTER) + "-" + end.format(FORMATTER);
        }
    }
}
