package com.xiaoxiao.arissweeping.command;

import com.xiaoxiao.arissweeping.command.Command.CommandResult;
import com.xiaoxiao.arissweeping.command.Command.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CommandInvoker 单元测试
 */
@ExtendWith(MockitoExtension.class)
class CommandInvokerTest {
    
    @Mock
    private Command mockCommand;
    
    @Mock
    private Command mockCommand2;
    
    private CommandInvoker invoker;
    
    @BeforeEach
    void setUp() {
        invoker = new CommandInvoker();
        
        // 设置模拟命令的基本行为
        when(mockCommand.getName()).thenReturn("MockCommand");
        when(mockCommand.getDescription()).thenReturn("Mock command for testing");
        when(mockCommand.getEstimatedExecutionTime()).thenReturn(100L);
        when(mockCommand.getPriority()).thenReturn(Command.Priority.NORMAL);
        when(mockCommand.getType()).thenReturn(Command.CommandType.CLEANUP);
        when(mockCommand.canUndo()).thenReturn(true);
        
        when(mockCommand2.getName()).thenReturn("MockCommand2");
        when(mockCommand2.getDescription()).thenReturn("Second mock command");
        when(mockCommand2.getEstimatedExecutionTime()).thenReturn(200L);
        when(mockCommand2.getPriority()).thenReturn(Command.Priority.HIGH);
        when(mockCommand2.getType()).thenReturn(Command.CommandType.CONFIG);
        when(mockCommand2.canUndo()).thenReturn(false);
    }
    
    @Test
    void testExecuteCommand_Success() {
        CommandResult expectedResult = new CommandResult(true, "Success", null, 100L);
        when(mockCommand.execute()).thenReturn(expectedResult);
        
        ValidationResult validationResult = new ValidationResult(true, Collections.emptyList());
        when(mockCommand.validate()).thenReturn(validationResult);
        
        CommandResult result = invoker.executeCommand(mockCommand);
        
        assertTrue(result.isSuccess());
        assertEquals("Success", result.getMessage());
        assertEquals(100L, result.getExecutionTime());
        
        verify(mockCommand).validate();
        verify(mockCommand).execute();
        
        // 验证命令被添加到历史记录
        assertEquals(1, invoker.getExecutionHistory().size());
        assertEquals(mockCommand, invoker.getExecutionHistory().get(0));
    }
    
    @Test
    void testExecuteCommand_ValidationFailure() {
        ValidationResult validationResult = new ValidationResult(false, 
            Arrays.asList("Invalid parameter", "Missing required field"));
        when(mockCommand.validate()).thenReturn(validationResult);
        
        CommandResult result = invoker.executeCommand(mockCommand);
        
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Validation failed"));
        assertTrue(result.getMessage().contains("Invalid parameter"));
        assertTrue(result.getMessage().contains("Missing required field"));
        
        verify(mockCommand).validate();
        verify(mockCommand, never()).execute();
        
        // 验证失败的命令不被添加到历史记录
        assertTrue(invoker.getExecutionHistory().isEmpty());
    }
    
    @Test
    void testExecuteCommand_ExecutionFailure() {
        ValidationResult validationResult = new ValidationResult(true, Collections.emptyList());
        when(mockCommand.validate()).thenReturn(validationResult);
        
        CommandResult expectedResult = new CommandResult(false, "Execution failed", 
            new RuntimeException("Test exception"), 50L);
        when(mockCommand.execute()).thenReturn(expectedResult);
        
        CommandResult result = invoker.executeCommand(mockCommand);
        
        assertFalse(result.isSuccess());
        assertEquals("Execution failed", result.getMessage());
        assertNotNull(result.getException());
        assertEquals(50L, result.getExecutionTime());
        
        // 验证失败的命令仍然被添加到历史记录
        assertEquals(1, invoker.getExecutionHistory().size());
    }
    
    @Test
    void testExecuteCommandAsync() throws Exception {
        CommandResult expectedResult = new CommandResult(true, "Async success", null, 150L);
        when(mockCommand.execute()).thenReturn(expectedResult);
        
        ValidationResult validationResult = new ValidationResult(true, Collections.emptyList());
        when(mockCommand.validate()).thenReturn(validationResult);
        
        CompletableFuture<CommandResult> future = invoker.executeCommandAsync(mockCommand);
        CommandResult result = future.get(1, TimeUnit.SECONDS);
        
        assertTrue(result.isSuccess());
        assertEquals("Async success", result.getMessage());
        assertEquals(150L, result.getExecutionTime());
        
        verify(mockCommand).validate();
        verify(mockCommand).execute();
    }
    
    @Test
    void testExecuteBatch_Success() {
        List<Command> commands = Arrays.asList(mockCommand, mockCommand2);
        
        CommandResult result1 = new CommandResult(true, "Command 1 success", null, 100L);
        CommandResult result2 = new CommandResult(true, "Command 2 success", null, 200L);
        
        ValidationResult validation1 = new ValidationResult(true, Collections.emptyList());
        ValidationResult validation2 = new ValidationResult(true, Collections.emptyList());
        
        when(mockCommand.validate()).thenReturn(validation1);
        when(mockCommand.execute()).thenReturn(result1);
        when(mockCommand2.validate()).thenReturn(validation2);
        when(mockCommand2.execute()).thenReturn(result2);
        
        List<CommandResult> results = invoker.executeBatch(commands);
        
        assertEquals(2, results.size());
        assertTrue(results.get(0).isSuccess());
        assertTrue(results.get(1).isSuccess());
        assertEquals("Command 1 success", results.get(0).getMessage());
        assertEquals("Command 2 success", results.get(1).getMessage());
        
        // 验证所有命令都被添加到历史记录
        assertEquals(2, invoker.getExecutionHistory().size());
    }
    
    @Test
    void testExecuteBatch_PartialFailure() {
        List<Command> commands = Arrays.asList(mockCommand, mockCommand2);
        
        CommandResult result1 = new CommandResult(true, "Command 1 success", null, 100L);
        CommandResult result2 = new CommandResult(false, "Command 2 failed", 
            new RuntimeException("Test failure"), 200L);
        
        ValidationResult validation1 = new ValidationResult(true, Collections.emptyList());
        ValidationResult validation2 = new ValidationResult(true, Collections.emptyList());
        
        when(mockCommand.validate()).thenReturn(validation1);
        when(mockCommand.execute()).thenReturn(result1);
        when(mockCommand2.validate()).thenReturn(validation2);
        when(mockCommand2.execute()).thenReturn(result2);
        
        List<CommandResult> results = invoker.executeBatch(commands);
        
        assertEquals(2, results.size());
        assertTrue(results.get(0).isSuccess());
        assertFalse(results.get(1).isSuccess());
        assertEquals("Command 1 success", results.get(0).getMessage());
        assertEquals("Command 2 failed", results.get(1).getMessage());
    }
    
    @Test
    void testExecuteBatchAsync() throws Exception {
        List<Command> commands = Arrays.asList(mockCommand, mockCommand2);
        
        CommandResult result1 = new CommandResult(true, "Async command 1", null, 100L);
        CommandResult result2 = new CommandResult(true, "Async command 2", null, 200L);
        
        ValidationResult validation1 = new ValidationResult(true, Collections.emptyList());
        ValidationResult validation2 = new ValidationResult(true, Collections.emptyList());
        
        when(mockCommand.validate()).thenReturn(validation1);
        when(mockCommand.execute()).thenReturn(result1);
        when(mockCommand2.validate()).thenReturn(validation2);
        when(mockCommand2.execute()).thenReturn(result2);
        
        CompletableFuture<List<CommandResult>> future = invoker.executeBatchAsync(commands);
        List<CommandResult> results = future.get(2, TimeUnit.SECONDS);
        
        assertEquals(2, results.size());
        assertTrue(results.get(0).isSuccess());
        assertTrue(results.get(1).isSuccess());
    }
    
    @Test
    void testUndoLastCommand_Success() {
        CommandResult executeResult = new CommandResult(true, "Execute success", null, 100L);
        CommandResult undoResult = new CommandResult(true, "Undo success", null, 50L);
        
        ValidationResult validationResult = new ValidationResult(true, Collections.emptyList());
        when(mockCommand.validate()).thenReturn(validationResult);
        when(mockCommand.execute()).thenReturn(executeResult);
        when(mockCommand.undo()).thenReturn(undoResult);
        
        // 先执行命令
        invoker.executeCommand(mockCommand);
        
        // 然后撤销
        Optional<CommandResult> result = invoker.undoLastCommand();
        
        assertTrue(result.isPresent());
        assertTrue(result.get().isSuccess());
        assertEquals("Undo success", result.get().getMessage());
        
        verify(mockCommand).undo();
        
        // 验证历史记录为空（命令被撤销）
        assertTrue(invoker.getExecutionHistory().isEmpty());
    }
    
    @Test
    void testUndoLastCommand_NoHistory() {
        Optional<CommandResult> result = invoker.undoLastCommand();
        
        assertFalse(result.isPresent());
    }
    
    @Test
    void testUndoLastCommand_CannotUndo() {
        CommandResult executeResult = new CommandResult(true, "Execute success", null, 100L);
        ValidationResult validationResult = new ValidationResult(true, Collections.emptyList());
        
        when(mockCommand.validate()).thenReturn(validationResult);
        when(mockCommand.execute()).thenReturn(executeResult);
        when(mockCommand.canUndo()).thenReturn(false);
        
        // 执行不可撤销的命令
        invoker.executeCommand(mockCommand);
        
        Optional<CommandResult> result = invoker.undoLastCommand();
        
        assertFalse(result.isPresent());
        verify(mockCommand, never()).undo();
        
        // 验证命令仍在历史记录中
        assertEquals(1, invoker.getExecutionHistory().size());
    }
    
    @Test
    void testUndoCommand_Success() {
        CommandResult executeResult = new CommandResult(true, "Execute success", null, 100L);
        CommandResult undoResult = new CommandResult(true, "Undo success", null, 50L);
        
        ValidationResult validationResult = new ValidationResult(true, Collections.emptyList());
        when(mockCommand.validate()).thenReturn(validationResult);
        when(mockCommand.execute()).thenReturn(executeResult);
        when(mockCommand.undo()).thenReturn(undoResult);
        
        // 执行命令
        invoker.executeCommand(mockCommand);
        
        // 撤销指定命令
        Optional<CommandResult> result = invoker.undoCommand(mockCommand);
        
        assertTrue(result.isPresent());
        assertTrue(result.get().isSuccess());
        assertEquals("Undo success", result.get().getMessage());
        
        verify(mockCommand).undo();
    }
    
    @Test
    void testUndoCommand_NotInHistory() {
        Optional<CommandResult> result = invoker.undoCommand(mockCommand);
        
        assertFalse(result.isPresent());
        verify(mockCommand, never()).undo();
    }
    
    @Test
    void testGetExecutionHistory() {
        CommandResult result1 = new CommandResult(true, "Success 1", null, 100L);
        CommandResult result2 = new CommandResult(true, "Success 2", null, 200L);
        
        ValidationResult validation = new ValidationResult(true, Collections.emptyList());
        when(mockCommand.validate()).thenReturn(validation);
        when(mockCommand.execute()).thenReturn(result1);
        when(mockCommand2.validate()).thenReturn(validation);
        when(mockCommand2.execute()).thenReturn(result2);
        
        invoker.executeCommand(mockCommand);
        invoker.executeCommand(mockCommand2);
        
        List<Command> history = invoker.getExecutionHistory();
        
        assertEquals(2, history.size());
        assertEquals(mockCommand, history.get(0));
        assertEquals(mockCommand2, history.get(1));
    }
    
    @Test
    void testClearHistory() {
        CommandResult result = new CommandResult(true, "Success", null, 100L);
        ValidationResult validation = new ValidationResult(true, Collections.emptyList());
        
        when(mockCommand.validate()).thenReturn(validation);
        when(mockCommand.execute()).thenReturn(result);
        
        invoker.executeCommand(mockCommand);
        assertEquals(1, invoker.getExecutionHistory().size());
        
        invoker.clearHistory();
        assertTrue(invoker.getExecutionHistory().isEmpty());
    }
    
    @Test
    void testGetStatistics() {
        CommandResult successResult = CommandResult.success("Success", null, 100L);
        CommandResult failureResult = CommandResult.failure("Failure", 
            new RuntimeException("Test"), 50L);
        
        ValidationResult validation = new ValidationResult(true, Collections.emptyList());
        when(mockCommand.validate()).thenReturn(validation);
        when(mockCommand.execute()).thenReturn(successResult);
        when(mockCommand2.validate()).thenReturn(validation);
        when(mockCommand2.execute()).thenReturn(failureResult);
        
        invoker.executeCommand(mockCommand);
        invoker.executeCommand(mockCommand2);
        
        CommandInvoker.InvokerStatistics stats = invoker.getStatistics();
        
        assertEquals(2, stats.getTotalExecutions());
        assertEquals(1, stats.getSuccessfulExecutions());
        assertEquals(1, stats.getFailedExecutions());
        assertEquals(50.0, stats.getSuccessRate(), 0.01);
        assertEquals(75.0, stats.getAverageExecutionTime(), 0.01); // (100 + 50) / 2
        assertTrue(stats.getTotalExecutionTime() >= 150);
    }
    
    @Test
    void testHistoryLimit() {
        CommandInvoker limitedInvoker = new CommandInvoker(3); // 限制历史记录为3条
        
        CommandResult result = new CommandResult(true, "Success", null, 100L);
        ValidationResult validation = new ValidationResult(true, Collections.emptyList());
        
        // 创建4个不同的命令
        Command cmd1 = mock(Command.class);
        Command cmd2 = mock(Command.class);
        Command cmd3 = mock(Command.class);
        Command cmd4 = mock(Command.class);
        
        for (Command cmd : Arrays.asList(cmd1, cmd2, cmd3, cmd4)) {
            when(cmd.validate()).thenReturn(validation);
            when(cmd.execute()).thenReturn(result);
            when(cmd.getName()).thenReturn("Command");
            when(cmd.getDescription()).thenReturn("Test command");
            when(cmd.getEstimatedExecutionTime()).thenReturn(100L);
            when(cmd.getPriority()).thenReturn(Command.Priority.NORMAL);
            when(cmd.getType()).thenReturn(Command.CommandType.CLEANUP);
            when(cmd.canUndo()).thenReturn(true);
        }
        
        // 执行4个命令
        limitedInvoker.executeCommand(cmd1);
        limitedInvoker.executeCommand(cmd2);
        limitedInvoker.executeCommand(cmd3);
        limitedInvoker.executeCommand(cmd4);
        
        List<Command> history = limitedInvoker.getExecutionHistory();
        
        // 应该只保留最后3个命令
        assertEquals(3, history.size());
        assertEquals(cmd2, history.get(0));
        assertEquals(cmd3, history.get(1));
        assertEquals(cmd4, history.get(2));
    }
    
    @Test
    void testShutdown() {
        CommandResult result = new CommandResult(true, "Success", null, 100L);
        ValidationResult validation = new ValidationResult(true, Collections.emptyList());
        
        when(mockCommand.validate()).thenReturn(validation);
        when(mockCommand.execute()).thenReturn(result);
        
        invoker.executeCommand(mockCommand);
        
        invoker.shutdown();
        
        // 验证历史记录被清空
        assertTrue(invoker.getExecutionHistory().isEmpty());
        
        // 验证统计信息被重置
        CommandInvoker.InvokerStatistics stats = invoker.getStatistics();
        assertEquals(0, stats.getTotalExecutions());
    }
    
    @Test
    void testConcurrentExecution() throws InterruptedException {
        CommandResult result = new CommandResult(true, "Success", null, 100L);
        ValidationResult validation = new ValidationResult(true, Collections.emptyList());
        
        List<Thread> threads = new ArrayList<>();
        
        // 创建多个线程同时执行命令
        for (int i = 0; i < 10; i++) {
            final int index = i;
            Thread thread = new Thread(() -> {
                Command cmd = mock(Command.class);
                when(cmd.validate()).thenReturn(validation);
                when(cmd.execute()).thenReturn(result);
                when(cmd.getName()).thenReturn("Command" + index);
                when(cmd.getDescription()).thenReturn("Test command " + index);
                when(cmd.getEstimatedExecutionTime()).thenReturn(100L);
                when(cmd.getPriority()).thenReturn(Command.Priority.NORMAL);
                when(cmd.getType()).thenReturn(Command.CommandType.CLEANUP);
                when(cmd.canUndo()).thenReturn(true);
                
                invoker.executeCommand(cmd);
            });
            threads.add(thread);
        }
        
        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
        
        // 验证所有命令都被执行
        CommandInvoker.InvokerStatistics stats = invoker.getStatistics();
        assertEquals(10, stats.getTotalExecutions());
        assertEquals(10, stats.getSuccessfulExecutions());
    }
}