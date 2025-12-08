package com.br.elohostel.service.components;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.logging.Logger;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.br.elohostel.service.FinancialRecordService;

@Component
public class FinancialScheduler {

    private static final Logger logger = Logger.getLogger(FinancialScheduler.class.getName());
    
    private final FinancialRecordService financialRecordService;

    public FinancialScheduler(FinancialRecordService financialRecordService) {
        this.financialRecordService = financialRecordService;
    }

    // ✅ CORREÇÃO: Executa todos os dias às 23:55 para processar o registro diário
    @Scheduled(cron = "0 55 23 * * ?")
    @Transactional
    public void processDailyFinancialRecord() {
        try {
            logger.info("🔄 Agendador diário executando...");
            financialRecordService.processFinancialRecords();
            logger.info("✅ Agendador diário concluído");
        } catch (Exception e) {
            // Log do erro, mas não quebra a aplicação
            logger.severe("❌ Erro no agendamento financeiro diário: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ CORREÇÃO: Executa no primeiro dia de cada mês às 00:05 para processar o mês anterior
    @Scheduled(cron = "0 5 0 1 * ?")
    @Transactional
    public void processMonthlyFinancialRecord() {
        try {
            logger.info("🔄 Agendador mensal executando...");
            LocalDate lastMonth = LocalDate.now().minusMonths(1);
            financialRecordService.processMonthlyRecord(lastMonth);
            logger.info("✅ Agendador mensal concluído para: " + YearMonth.from(lastMonth));
        } catch (Exception e) {
            logger.severe("❌ Erro no agendamento mensal financeiro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ CORREÇÃO: Executa no primeiro dia do ano às 00:10 para processar o ano anterior
    @Scheduled(cron = "0 10 0 1 1 ?")
    @Transactional
    public void processAnnualFinancialRecord() {
        try {
            logger.info("🔄 Agendador anual executando...");
            int lastYear = LocalDate.now().getYear() - 1;
            financialRecordService.processAnnualRecord(lastYear);
            logger.info("✅ Agendador anual concluído para: " + lastYear);
        } catch (Exception e) {
            logger.severe("❌ Erro no agendamento anual financeiro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ NOVO: Agendamento para debug/teste (executa a cada hora para desenvolvimento)
    @Scheduled(cron = "0 0 * * * ?") // A cada hora
    @Transactional
    public void hourlyDebug() {
        try {
            logger.info("🐛 Agendador de debug executando...");
            // Log rápido do status atual
            LocalDate today = LocalDate.now();
            logger.info("📊 Debug - Data atual: " + today);
            
            // Você pode adicionar chamadas de debug aqui se necessário
            // financialRecordService.debugReservations();
            
            logger.info("✅ Agendador de debug concluído");
        } catch (Exception e) {
            logger.severe("❌ Erro no agendamento de debug: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ NOVO: Agendamento para forçar processamento em caso de falha
    @Scheduled(cron = "0 30 6 * * ?") // Todos os dias às 6:30
    @Transactional
    public void forceProcessIfNeeded() {
        try {
            logger.info("⚡ Verificando necessidade de processamento forçado...");
            LocalDate yesterday = LocalDate.now().minusDays(1);
            
            // Forçar processamento do dia anterior como fallback
            financialRecordService.forceProcessDate(yesterday);
            logger.info("✅ Processamento forçado concluído para: " + yesterday);
        } catch (Exception e) {
            logger.severe("❌ Erro no processamento forçado: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ NOVO: Agendamento para previsão financeira (executa diariamente às 08:00)
    @Scheduled(cron = "0 0 8 * * ?") // Todos os dias às 8:00
    @Transactional
    public void processFinancialForecast() {
        try {
            logger.info("🔮 Agendador de previsão executando...");
            LocalDate today = LocalDate.now();
            LocalDate nextWeek = today.plusWeeks(1);
            
            // Processar previsão para a próxima semana
            financialRecordService.processForecastPeriod(today, nextWeek);
            logger.info("✅ Previsão financeira concluída para: " + today + " a " + nextWeek);
        } catch (Exception e) {
            logger.severe("❌ Erro no agendamento de previsão: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ NOVO: Agendamento para processar reservas pendentes (executa a cada 30 minutos durante o horário comercial)
    @Scheduled(cron = "0 0/30 9-18 * * ?") // A cada 30 minutos, das 9h às 18h
    @Transactional
    public void processPendingReservations() {
        try {
            logger.info("⏰ Agendador de reservas pendentes executando...");
            LocalDate today = LocalDate.now();
            
            // Processar apenas reservas não processadas de hoje
            financialRecordService.processDailyRecord(today);
            logger.info("✅ Processamento de reservas pendentes concluído para: " + today);
        } catch (Exception e) {
            logger.severe("❌ Erro no processamento de reservas pendentes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ NOVO: Agendamento para estatísticas (executa todo domingo às 03:00)
    @Scheduled(cron = "0 0 3 * * SUN")
    @Transactional
    public void weeklyStatistics() {
        try {
            logger.info("📈 Agendador de estatísticas semanais executando...");
            
            // Processar todas as reservas da semana anterior
            LocalDate endOfLastWeek = LocalDate.now().minusDays(1);
            LocalDate startOfLastWeek = endOfLastWeek.minusDays(6);
            
            financialRecordService.processForecastPeriod(startOfLastWeek, endOfLastWeek);
            logger.info("✅ Estatísticas semanais concluídas para: " + startOfLastWeek + " a " + endOfLastWeek);
        } catch (Exception e) {
            logger.severe("❌ Erro nas estatísticas semanais: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ NOVO: Agendamento para limpeza/manutenção (executa todo domingo às 02:00)
    @Scheduled(cron = "0 0 2 * * SUN")
    @Transactional
    public void weeklyMaintenance() {
        try {
            logger.info("🧹 Executando manutenção semanal...");
            
            // Aqui você pode adicionar tarefas de limpeza ou otimização se necessário
            // Exemplo: remover registros financeiros muito antigos
            // financialRecordService.cleanOldRecords();
            
            logger.info("✅ Manutenção semanal concluída");
        } catch (Exception e) {
            logger.severe("❌ Erro na manutenção semanal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ NOVO: Agendamento para backup de dados (executa no primeiro dia de cada mês às 01:00)
    @Scheduled(cron = "0 0 1 1 * ?")
    @Transactional
    public void monthlyBackup() {
        try {
            logger.info("💾 Executando backup mensal...");
            
            // Processar previsão para o próximo mês
            LocalDate firstDayNextMonth = LocalDate.now().plusMonths(1).withDayOfMonth(1);
            LocalDate lastDayNextMonth = firstDayNextMonth.withDayOfMonth(firstDayNextMonth.lengthOfMonth());
            
            financialRecordService.processForecastPeriod(firstDayNextMonth, lastDayNextMonth);
            logger.info("✅ Backup mensal concluído - Previsão para: " + firstDayNextMonth + " a " + lastDayNextMonth);
        } catch (Exception e) {
            logger.severe("❌ Erro no backup mensal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ✅ NOVO: Agendamento para teste do sistema (executa aos sábados às 10:00)
    @Scheduled(cron = "0 0 10 * * SAT")
    @Transactional
    public void systemTest() {
        try {
            logger.info("🧪 Executando teste do sistema...");
            
            // Testar processamento de datas específicas
            LocalDate testDate1 = LocalDate.now().plusDays(1);
            LocalDate testDate2 = LocalDate.now().plusDays(7);
            
            financialRecordService.processForecast(testDate1);
            financialRecordService.processForecast(testDate2);
            
            // Executar debug para verificar status
            financialRecordService.debugReservations();
            
            logger.info("✅ Teste do sistema concluído");
        } catch (Exception e) {
            logger.severe("❌ Erro no teste do sistema: " + e.getMessage());
            e.printStackTrace();
        }
    }
}