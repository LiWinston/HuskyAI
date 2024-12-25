// 各编程语言的代表性代码示例
export const CODE_EXAMPLES = {
    java: {
        name: 'Java (Spring)',
        code: `@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Value("\${app.user.default-role}")
    private String defaultRole;
    
    @Cacheable(value = "users", key = "#userId")
    @Override
    public UserDTO findById(Long userId) {
        return userRepository.findById(userId)
            .map(this::convertToDTO)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
    
    @Transactional
    @Override
    public UserDTO createUser(UserCreateRequest request) {
        validateRequest(request);
        
        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .role(defaultRole)
            .status(UserStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .build();
            
        User savedUser = userRepository.save(user);
        kafkaTemplate.send("user-events", "user-created", 
            objectMapper.writeValueAsString(savedUser));
            
        log.info("Created new user: {}", savedUser.getUsername());
        return convertToDTO(savedUser);
    }
    
    @Async
    public CompletableFuture<List<UserDTO>> findAllAsync() {
        return CompletableFuture.supplyAsync(() -> 
            userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList())
        );
    }
    
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupInactiveUsers() {
        userRepository.deleteByStatusAndLastLoginBefore(
            UserStatus.INACTIVE,
            LocalDateTime.now().minusMonths(6)
        );
    }
}`
    },
    python: {
        name: 'Python (Deep Learning)',
        code: `import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader
from torchvision import transforms
import pytorch_lightning as pl

class AttentionBlock(nn.Module):
    def __init__(self, embed_dim, num_heads):
        super().__init__()
        self.attention = nn.MultiheadAttention(embed_dim, num_heads)
        self.norm1 = nn.LayerNorm(embed_dim)
        self.norm2 = nn.LayerNorm(embed_dim)
        self.ffn = nn.Sequential(
            nn.Linear(embed_dim, 4 * embed_dim),
            nn.GELU(),
            nn.Linear(4 * embed_dim, embed_dim)
        )

    def forward(self, x):
        attention_output, _ = self.attention(x, x, x)
        x = self.norm1(x + attention_output)
        ffn_output = self.ffn(x)
        return self.norm2(x + ffn_output)

class VisionTransformer(pl.LightningModule):
    def __init__(self, image_size=224, patch_size=16, num_classes=1000,
                 embed_dim=768, depth=12, num_heads=12):
        super().__init__()
        self.patch_embed = nn.Conv2d(3, embed_dim, kernel_size=patch_size, stride=patch_size)
        self.cls_token = nn.Parameter(torch.zeros(1, 1, embed_dim))
        self.pos_embed = nn.Parameter(torch.zeros(1, (image_size // patch_size) ** 2 + 1, embed_dim))
        
        self.blocks = nn.ModuleList([
            AttentionBlock(embed_dim, num_heads) for _ in range(depth)
        ])
        
        self.norm = nn.LayerNorm(embed_dim)
        self.head = nn.Linear(embed_dim, num_classes)
        
        self.criterion = nn.CrossEntropyLoss()

    def forward(self, x):
        B = x.shape[0]
        x = self.patch_embed(x).flatten(2).transpose(1, 2)
        cls_tokens = self.cls_token.expand(B, -1, -1)
        x = torch.cat((cls_tokens, x), dim=1)
        x = x + self.pos_embed
        
        for block in self.blocks:
            x = block(x)
            
        x = self.norm(x)
        x = self.head(x[:, 0])
        return x

    def training_step(self, batch, batch_idx):
        images, labels = batch
        logits = self(images)
        loss = self.criterion(logits, labels)
        self.log('train_loss', loss)
        return loss

    def configure_optimizers(self):
        optimizer = optim.AdamW(self.parameters(), lr=3e-4, weight_decay=0.1)
        scheduler = optim.lr_scheduler.CosineAnnealingLR(optimizer, T_max=100)
        return [optimizer], [scheduler]

# 数据加载和训练
transform = transforms.Compose([
    transforms.RandomResizedCrop(224),
    transforms.RandomHorizontalFlip(),
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406],
                       std=[0.229, 0.224, 0.225])
])`
    },
    csharp: {
        name: 'C# (Unity)',
        code: `using UnityEngine;
using System.Collections;
using System.Collections.Generic;
using UnityEngine.Events;

[RequireComponent(typeof(Rigidbody))]
public class PlayerController : MonoBehaviour
{
    [Header("Movement Settings")]
    [SerializeField] private float moveSpeed = 5f;
    [SerializeField] private float jumpForce = 8f;
    [SerializeField] private float rotationSpeed = 2f;
    
    [Header("Ground Check")]
    [SerializeField] private LayerMask groundLayer;
    [SerializeField] private float groundCheckDistance = 0.2f;
    
    [Header("Combat")]
    [SerializeField] private int maxHealth = 100;
    [SerializeField] private GameObject weaponPrefab;
    
    private Rigidbody rb;
    private Animator animator;
    private bool isGrounded;
    private int currentHealth;
    private readonly List<IWeapon> weapons = new List<IWeapon>();
    
    public UnityEvent<int> onHealthChanged;
    
    private void Awake()
    {
        rb = GetComponent<Rigidbody>();
        animator = GetComponent<Animator>();
        currentHealth = maxHealth;
        
        // 注册事件监听
        GameEvents.Instance.onGamePaused += HandleGamePaused;
    }
    
    private void Update()
    {
        HandleMovement();
        HandleJump();
        HandleWeaponSwitch();
    }
    
    private void HandleMovement()
    {
        float horizontal = Input.GetAxisRaw("Horizontal");
        float vertical = Input.GetAxisRaw("Vertical");
        
        Vector3 movement = new Vector3(horizontal, 0f, vertical).normalized;
        
        if (movement.magnitude > 0.1f)
        {
            float targetAngle = Mathf.Atan2(movement.x, movement.z) * Mathf.Rad2Deg;
            float angle = Mathf.LerpAngle(transform.eulerAngles.y, targetAngle, 
                Time.deltaTime * rotationSpeed);
                
            transform.rotation = Quaternion.Euler(0f, angle, 0f);
            rb.MovePosition(transform.position + movement * moveSpeed * Time.deltaTime);
            
            animator.SetFloat("Speed", movement.magnitude);
        }
    }
    
    private void HandleJump()
    {
        isGrounded = Physics.Raycast(transform.position, Vector3.down, 
            groundCheckDistance, groundLayer);
            
        if (isGrounded && Input.GetButtonDown("Jump"))
        {
            rb.AddForce(Vector3.up * jumpForce, ForceMode.Impulse);
            animator.SetTrigger("Jump");
        }
    }
    
    public void TakeDamage(int damage)
    {
        currentHealth = Mathf.Max(0, currentHealth - damage);
        onHealthChanged?.Invoke(currentHealth);
        
        if (currentHealth <= 0)
        {
            StartCoroutine(HandleDeath());
        }
    }
    
    private IEnumerator HandleDeath()
    {
        animator.SetTrigger("Death");
        enabled = false;
        yield return new WaitForSeconds(2f);
        GameEvents.Instance.TriggerGameOver();
    }
    
    private void OnDestroy()
    {
        GameEvents.Instance.onGamePaused -= HandleGamePaused;
    }
}`
    },
    sql: {
        name: 'SQL',
        code: `WITH UserStats AS (
    SELECT 
        u.user_id,
        u.username,
        COUNT(DISTINCT o.order_id) as total_orders,
        SUM(oi.quantity * p.price) as total_spent,
        FIRST_VALUE(o.order_date) OVER (
            PARTITION BY u.user_id 
            ORDER BY o.order_date
        ) as first_order_date,
        DENSE_RANK() OVER (
            ORDER BY SUM(oi.quantity * p.price) DESC
        ) as spending_rank
    FROM users u
    LEFT JOIN orders o ON u.user_id = o.user_id
    LEFT JOIN order_items oi ON o.order_id = oi.order_id
    LEFT JOIN products p ON oi.product_id = p.product_id
    WHERE o.order_date >= DATE_SUB(CURRENT_DATE, INTERVAL 1 YEAR)
    GROUP BY u.user_id, u.username
),
CategoryPreferences AS (
    SELECT 
        u.user_id,
        c.category_name,
        COUNT(*) as purchase_count,
        ROW_NUMBER() OVER (
            PARTITION BY u.user_id 
            ORDER BY COUNT(*) DESC
        ) as category_rank
    FROM users u
    JOIN orders o ON u.user_id = o.user_id
    JOIN order_items oi ON o.order_id = oi.order_id
    JOIN products p ON oi.product_id = p.product_id
    JOIN categories c ON p.category_id = c.category_id
    GROUP BY u.user_id, c.category_name
),
UserSegments AS (
    SELECT
        us.*,
        cp.category_name as favorite_category,
        CASE
            WHEN total_orders >= 10 AND total_spent >= 1000 THEN 'VIP'
            WHEN total_orders >= 5 OR total_spent >= 500 THEN 'Regular'
            ELSE 'New'
        END as customer_segment,
        NTILE(4) OVER (ORDER BY total_spent) as spending_quartile
    FROM UserStats us
    LEFT JOIN CategoryPreferences cp ON us.user_id = cp.user_id
    WHERE cp.category_rank = 1 OR cp.category_rank IS NULL
)
SELECT 
    us.*,
    r.review_score as avg_review_score,
    COALESCE(rf.referral_count, 0) as referral_count,
    CASE
        WHEN us.spending_quartile = 4 THEN 'High Value'
        WHEN us.spending_quartile = 3 THEN 'Mid-High Value'
        WHEN us.spending_quartile = 2 THEN 'Mid-Low Value'
        ELSE 'Low Value'
    END as value_segment
FROM UserSegments us
LEFT JOIN (
    SELECT 
        o.user_id,
        AVG(r.rating) as review_score
    FROM orders o
    JOIN reviews r ON o.order_id = r.order_id
    GROUP BY o.user_id
) r ON us.user_id = r.user_id
LEFT JOIN (
    SELECT 
        referrer_id,
        COUNT(*) as referral_count
    FROM user_referrals
    WHERE referral_date >= DATE_SUB(CURRENT_DATE, INTERVAL 1 YEAR)
    GROUP BY referrer_id
) rf ON us.user_id = rf.referrer_id
WHERE us.total_orders > 0
ORDER BY us.spending_rank, us.total_orders DESC
LIMIT 100;`
    },
    typescript: {
        name: 'TypeScript (React)',
        code: `import React, { useEffect, useCallback, useRef } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { ThunkDispatch } from 'redux-thunk';
import { motion, AnimatePresence } from 'framer-motion';

interface User {
    id: string;
    name: string;
    email: string;
    preferences: UserPreferences;
}

interface UserPreferences {
    theme: 'light' | 'dark' | 'system';
    notifications: NotificationSettings;
    accessibility: AccessibilityOptions;
}

interface NotificationSettings {
    email: boolean;
    push: boolean;
    frequency: 'immediate' | 'daily' | 'weekly';
}

interface AccessibilityOptions {
    fontSize: number;
    contrast: 'normal' | 'high';
    reduceMotion: boolean;
}

type AppState = {
    users: User[];
    loading: boolean;
    error: string | null;
};

const UserDashboard: React.FC = () => {
    const dispatch = useDispatch<ThunkDispatch<AppState, void, any>>();
    const users = useSelector((state: AppState) => state.users);
    const loading = useSelector((state: AppState) => state.loading);
    const containerRef = useRef<HTMLDivElement>(null);
    
    const fetchUsers = useCallback(async () => {
        try {
            dispatch({ type: 'FETCH_USERS_START' });
            const response = await fetch('/api/users');
            const data: User[] = await response.json();
            
            dispatch({ 
                type: 'FETCH_USERS_SUCCESS',
                payload: data
            });
        } catch (error) {
            dispatch({ 
                type: 'FETCH_USERS_ERROR',
                payload: error.message
            });
        }
    }, [dispatch]);
    
    useEffect(() => {
        fetchUsers();
        
        const handleResize = debounce(() => {
            if (containerRef.current) {
                adjustLayout(containerRef.current);
            }
        }, 250);
        
        window.addEventListener('resize', handleResize);
        return () => window.removeEventListener('resize', handleResize);
    }, [fetchUsers]);
    
    const handleUserPreferences = useCallback(<T extends keyof UserPreferences>(
        userId: string,
        category: T,
        value: UserPreferences[T]
    ) => {
        dispatch(updateUserPreferences(userId, category, value));
    }, [dispatch]);
    
    return (
        <div ref={containerRef} className="dashboard-container">
            <AnimatePresence>
                {loading ? (
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        className="loading-overlay"
                    >
                        <LoadingSpinner />
                    </motion.div>
                ) : (
                    <motion.div
                        layout
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.3 }}
                    >
                        {users.map(user => (
                            <UserCard
                                key={user.id}
                                user={user}
                                onPreferenceChange={handleUserPreferences}
                                layoutId={user.id}
                            />
                        ))}
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
};

export const debounce = <T extends (...args: any[]) => any>(
    func: T,
    wait: number
): ((...args: Parameters<T>) => void) => {
    let timeout: NodeJS.Timeout;
    
    return (...args: Parameters<T>) => {
        clearTimeout(timeout);
        timeout = setTimeout(() => func(...args), wait);
    };
};

export default UserDashboard;`
    },
    javascript: {
        name: 'JavaScript (Node.js)',
        code: `const express = require('express');
const { createServer } = require('http');
const WebSocket = require('ws');
const Redis = require('ioredis');
const { RateLimiterRedis } = require('rate-limiter-flexible');

const app = express();
const server = createServer(app);
const wss = new WebSocket.Server({ server });

// Redis配置
const redis = new Redis({
    host: process.env.REDIS_HOST,
    port: process.env.REDIS_PORT,
    retryStrategy: (times) => Math.min(times * 50, 2000)
});

// 速率限制器配置
const rateLimiter = new RateLimiterRedis({
    storeClient: redis,
    keyPrefix: 'ratelimit',
    points: 10,
    duration: 1
});

// WebSocket连接池
const clients = new Map();

// 消息队列处理
const messageQueue = async (message) => {
    try {
        await redis.publish('chat_messages', JSON.stringify(message));
        await redis.lpush('message_history', JSON.stringify(message));
        await redis.ltrim('message_history', 0, 99); // 保留最近100条消息
    } catch (error) {
        console.error('Message queue error:', error);
    }
};

// WebSocket连接处理
wss.on('connection', async (ws, req) => {
    const clientId = req.headers['sec-websocket-key'];
    const clientIp = req.headers['x-forwarded-for'] || req.connection.remoteAddress;
    
    try {
        await rateLimiter.consume(clientIp);
        
        clients.set(clientId, {
            ws,
            ip: clientIp,
            lastActivity: Date.now()
        });
        
        // 发送历史消息
        const history = await redis.lrange('message_history', 0, -1);
        history.reverse().forEach(msg => {
            ws.send(msg);
        });
        
        ws.on('message', async (data) => {
            try {
                const message = JSON.parse(data);
                
                // 消息验证和清理
                const sanitizedMessage = {
                    id: crypto.randomUUID(),
                    text: sanitizeHtml(message.text),
                    userId: message.userId,
                    timestamp: Date.now()
                };
                
                // 广播消息
                const broadcastMessage = JSON.stringify(sanitizedMessage);
                clients.forEach((client) => {
                    if (client.ws.readyState === WebSocket.OPEN) {
                        client.ws.send(broadcastMessage);
                    }
                });
                
                // 异步处理消息
                await messageQueue(sanitizedMessage);
                
            } catch (error) {
                console.error('Message processing error:', error);
                ws.send(JSON.stringify({
                    type: 'error',
                    message: 'Message processing failed'
                }));
            }
        });
        
        // 心跳检测
        const pingInterval = setInterval(() => {
            if (ws.readyState === WebSocket.OPEN) {
                ws.ping();
            }
        }, 30000);
        
        ws.on('close', () => {
            clearInterval(pingInterval);
            clients.delete(clientId);
        });
        
    } catch (error) {
        ws.send(JSON.stringify({
            type: 'error',
            message: 'Rate limit exceeded'
        }));
        ws.close();
    }
});

// 定期清理断开的连接
setInterval(() => {
    const now = Date.now();
    clients.forEach((client, id) => {
        if (now - client.lastActivity > 60000) {
            client.ws.terminate();
            clients.delete(id);
        }
    });
}, 60000);

// 错误处理中间件
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({
        error: 'Internal Server Error',
        message: process.env.NODE_ENV === 'development' ? err.message : undefined
    });
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
    console.log(\`Server running on port \${PORT}\`);
});`
    },
    rust: {
        name: 'Rust (Systems)',
        code: `use tokio::{self, sync::{mpsc, RwLock}};
use std::{
    collections::HashMap,
    sync::Arc,
    time::{Duration, Instant},
};
use serde::{Deserialize, Serialize};
use anyhow::Result;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CacheEntry<T> {
    data: T,
    expiry: Instant,
}

#[derive(Debug)]
pub struct DistributedCache<T> {
    storage: Arc<RwLock<HashMap<String, CacheEntry<T>>>>,
    tx: mpsc::Sender<CacheCommand<T>>,
}

#[derive(Debug)]
enum CacheCommand<T> {
    Set(String, T, Duration),
    Remove(String),
    Clear,
}

impl<T: Clone + Send + Sync + 'static> DistributedCache<T> {
    pub async fn new() -> Result<Self> {
        let (tx, mut rx) = mpsc::channel(100);
        let storage = Arc::new(RwLock::new(HashMap::new()));
        let cleanup_storage = storage.clone();

        // 启动清理任务
        tokio::spawn(async move {
            let mut interval = tokio::time::interval(Duration::from_secs(60));
            loop {
                interval.tick().await;
                let mut cache = cleanup_storage.write().await;
                cache.retain(|_, entry| entry.expiry > Instant::now());
            }
        });

        // 启动命令处理任务
        let command_storage = storage.clone();
        tokio::spawn(async move {
            while let Some(cmd) = rx.recv().await {
                let mut cache = command_storage.write().await;
                match cmd {
                    CacheCommand::Set(key, value, ttl) => {
                        cache.insert(key, CacheEntry {
                            data: value,
                            expiry: Instant::now() + ttl,
                        });
                    }
                    CacheCommand::Remove(key) => {
                        cache.remove(&key);
                    }
                    CacheCommand::Clear => {
                        cache.clear();
                    }
                }
            }
        });

        Ok(Self { storage, tx })
    }

    pub async fn get(&self, key: &str) -> Option<T> {
        let cache = self.storage.read().await;
        cache.get(key)
            .filter(|entry| entry.expiry > Instant::now())
            .map(|entry| entry.data.clone())
    }

    pub async fn set(&self, key: String, value: T, ttl: Duration) -> Result<()> {
        self.tx.send(CacheCommand::Set(key, value, ttl)).await?;
        Ok(())
    }

    pub async fn remove(&self, key: String) -> Result<()> {
        self.tx.send(CacheCommand::Remove(key)).await?;
        Ok(())
    }

    pub async fn clear(&self) -> Result<()> {
        self.tx.send(CacheCommand::Clear).await?;
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use tokio::time::sleep;

    #[tokio::test]
    async fn test_cache_operations() -> Result<()> {
        let cache: DistributedCache<String> = DistributedCache::new().await?;

        // 测试设置和获取
        cache.set(
            "key1".to_string(), 
            "value1".to_string(),
            Duration::from_secs(2)
        ).await?;

        assert_eq!(
            cache.get("key1").await,
            Some("value1".to_string())
        );

        // 测试过期
        sleep(Duration::from_secs(3)).await;
        assert_eq!(cache.get("key1").await, None);

        // 测试删除
        cache.set(
            "key2".to_string(),
            "value2".to_string(),
            Duration::from_secs(60)
        ).await?;
        cache.remove("key2".to_string()).await?;
        assert_eq!(cache.get("key2").await, None);

        Ok(())
    }
}`
    },
    go: {
        name: 'Go (Microservices)',
        code: `package main

import (
    "context"
    "encoding/json"
    "log"
    "net/http"
    "os"
    "os/signal"
    "time"

    "github.com/go-redis/redis/v8"
    "github.com/gorilla/mux"
    "github.com/prometheus/client_golang/prometheus"
    "github.com/prometheus/client_golang/prometheus/promhttp"
    "go.opentelemetry.io/otel"
    "go.uber.org/zap"
)

type OrderService struct {
    logger  *zap.Logger
    redis   *redis.Client
    metrics *ServiceMetrics
}

type ServiceMetrics struct {
    requestDuration *prometheus.HistogramVec
    requestTotal    *prometheus.CounterVec
    cacheHits       *prometheus.CounterVec
}

type Order struct {
    ID        string    \`json:"id"\`
    UserID    string    \`json:"user_id"\`
    Items     []Item    \`json:"items"\`
    Total     float64   \`json:"total"\`
    Status    string    \`json:"status"\`
    CreatedAt time.Time \`json:"created_at"\`
}

type Item struct {
    ProductID string  \`json:"product_id"\`
    Quantity  int     \`json:"quantity"\`
    Price     float64 \`json:"price"\`
}

func NewOrderService(logger *zap.Logger, redis *redis.Client) *OrderService {
    metrics := &ServiceMetrics{
        requestDuration: prometheus.NewHistogramVec(
            prometheus.HistogramOpts{
                Name: "order_request_duration_seconds",
                Help: "Time spent processing requests",
            },
            []string{"method", "endpoint"},
        ),
        requestTotal: prometheus.NewCounterVec(
            prometheus.CounterOpts{
                Name: "order_requests_total",
                Help: "Total number of requests",
            },
            []string{"method", "endpoint", "status"},
        ),
        cacheHits: prometheus.NewCounterVec(
            prometheus.CounterOpts{
                Name: "order_cache_hits_total",
                Help: "Total number of cache hits",
            },
            []string{"operation"},
        ),
    }

    prometheus.MustRegister(
        metrics.requestDuration,
        metrics.requestTotal,
        metrics.cacheHits,
    )

    return &OrderService{
        logger:  logger,
        redis:   redis,
        metrics: metrics,
    }
}

func (s *OrderService) GetOrder(w http.ResponseWriter, r *http.Request) {
    ctx, span := otel.Tracer("order-service").Start(r.Context(), "GetOrder")
    defer span.End()

    timer := prometheus.NewTimer(s.metrics.requestDuration.WithLabelValues("GET", "/order"))
    defer timer.ObserveDuration()

    vars := mux.Vars(r)
    orderID := vars["id"]

    // 尝试从缓存获取
    cacheKey := "order:" + orderID
    cachedOrder, err := s.redis.Get(ctx, cacheKey).Result()
    if err == nil {
        s.metrics.cacheHits.WithLabelValues("get").Inc()
        w.Header().Set("Content-Type", "application/json")
        w.Write([]byte(cachedOrder))
        s.metrics.requestTotal.WithLabelValues("GET", "/order", "200").Inc()
        return
    }

    // 从数据库获取订单（简化示例）
    order := &Order{
        ID:        orderID,
        UserID:    "user123",
        Status:    "pending",
        CreatedAt: time.Now(),
        Items: []Item{
            {
                ProductID: "prod1",
                Quantity:  2,
                Price:     29.99,
            },
        },
        Total: 59.98,
    }

    // 缓存订单
    orderJSON, err := json.Marshal(order)
    if err != nil {
        s.handleError(w, err, "Failed to marshal order")
        return
    }

    if err := s.redis.Set(ctx, cacheKey, orderJSON, 15*time.Minute).Err(); err != nil {
        s.logger.Error("Failed to cache order", zap.Error(err))
    }

    w.Header().Set("Content-Type", "application/json")
    w.Write(orderJSON)
    s.metrics.requestTotal.WithLabelValues("GET", "/order", "200").Inc()
}

func (s *OrderService) handleError(w http.ResponseWriter, err error, msg string) {
    s.logger.Error(msg, zap.Error(err))
    http.Error(w, msg, http.StatusInternalServerError)
    s.metrics.requestTotal.WithLabelValues("GET", "/order", "500").Inc()
}

func main() {
    // 初始化日志
    logger, _ := zap.NewProduction()
    defer logger.Sync()

    // 初始化Redis客户端
    rdb := redis.NewClient(&redis.Options{
        Addr: "localhost:6379",
    })

    // 创建服务
    service := NewOrderService(logger, rdb)

    // 设置路由
    r := mux.NewRouter()
    r.HandleFunc("/orders/{id}", service.GetOrder).Methods("GET")
    r.Handle("/metrics", promhttp.Handler())

    // 配置服务器
    srv := &http.Server{
        Addr:         ":8080",
        Handler:      r,
        ReadTimeout:  10 * time.Second,
        WriteTimeout: 10 * time.Second,
    }

    // 优雅���闭
    go func() {
        sigChan := make(chan os.Signal, 1)
        signal.Notify(sigChan, os.Interrupt)
        <-sigChan

        ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
        defer cancel()

        if err := srv.Shutdown(ctx); err != nil {
            logger.Fatal("Server shutdown failed", zap.Error(err))
        }
    }()

    // 启动服务器
    logger.Info("Starting server on :8080")
    if err := srv.ListenAndServe(); err != http.ErrServerClosed {
        logger.Fatal("Server failed", zap.Error(err))
    }
}`
    },
    cpp: {
        name: 'C++ (Game Engine)',
        code: `#include <memory>
#include <vector>
#include <unordered_map>
#include <string>
#include <functional>
#include <chrono>
#include <glm/glm.hpp>

// 前向声明
class GameObject;
class Component;
class Transform;
class PhysicsSystem;
class RenderSystem;

// ECS（实体组件系统）基础类
class Entity {
public:
    using EntityID = std::uint32_t;
    
    explicit Entity(EntityID id) : id_(id) {}
    EntityID GetID() const { return id_; }
    
private:
    EntityID id_;
};

// 组件基类
class Component {
public:
    virtual ~Component() = default;
    virtual void Initialize() {}
    virtual void Update(float deltaTime) {}
    
    void SetOwner(GameObject* owner) { owner_ = owner; }
    GameObject* GetOwner() const { return owner_; }
    
protected:
    GameObject* owner_ = nullptr;
};

// 变换组件
class Transform : public Component {
public:
    void SetPosition(const glm::vec3& position) { position_ = position; }
    void SetRotation(const glm::quat& rotation) { rotation_ = rotation; }
    void SetScale(const glm::vec3& scale) { scale_ = scale; }
    
    glm::vec3 GetPosition() const { return position_; }
    glm::quat GetRotation() const { return rotation_; }
    glm::vec3 GetScale() const { return scale_; }
    
    glm::mat4 GetWorldMatrix() const {
        return glm::translate(glm::mat4(1.0f), position_) *
               glm::mat4_cast(rotation_) *
               glm::scale(glm::mat4(1.0f), scale_);
    }
    
private:
    glm::vec3 position_ = glm::vec3(0.0f);
    glm::quat rotation_ = glm::quat(1.0f, 0.0f, 0.0f, 0.0f);
    glm::vec3 scale_ = glm::vec3(1.0f);
};

// 物理组件
class RigidBody : public Component {
public:
    void SetMass(float mass) { mass_ = mass; }
    void SetVelocity(const glm::vec3& velocity) { velocity_ = velocity; }
    void ApplyForce(const glm::vec3& force) {
        if (mass_ > 0.0f) {
            velocity_ += force / mass_;
        }
    }
    
    void Update(float deltaTime) override {
        if (auto transform = GetOwner()->GetComponent<Transform>()) {
            auto position = transform->GetPosition();
            position += velocity_ * deltaTime;
            transform->SetPosition(position);
        }
    }
    
private:
    float mass_ = 1.0f;
    glm::vec3 velocity_ = glm::vec3(0.0f);
};

// 游戏对象类
class GameObject {
public:
    template<typename T, typename... Args>
    T* AddComponent(Args&&... args) {
        static_assert(std::is_base_of<Component, T>::value,
                     "T must inherit from Component");
                     
        auto component = std::make_unique<T>(std::forward<Args>(args)...);
        component->SetOwner(this);
        component->Initialize();
        
        T* componentPtr = component.get();
        components_[typeid(T)] = std::move(component);
        return componentPtr;
    }
    
    template<typename T>
    T* GetComponent() const {
        auto it = components_.find(typeid(T));
        return it != components_.end() ? 
               dynamic_cast<T*>(it->second.get()) : nullptr;
    }
    
    void Update(float deltaTime) {
        for (auto& [type, component] : components_) {
            component->Update(deltaTime);
        }
    }
    
private:
    std::unordered_map<
        std::type_index,
        std::unique_ptr<Component>
    > components_;
};

// 场景管理器
class Scene {
public:
    GameObject* CreateGameObject() {
        auto gameObject = std::make_unique<GameObject>();
        auto gameObjectPtr = gameObject.get();
        gameObjects_.push_back(std::move(gameObject));
        return gameObjectPtr;
    }
    
    void Update(float deltaTime) {
        for (auto& gameObject : gameObjects_) {
            gameObject->Update(deltaTime);
        }
    }
    
private:
    std::vector<std::unique_ptr<GameObject>> gameObjects_;
};

// 示例用法
int main() {
    Scene scene;
    
    // 创建玩家对象
    auto player = scene.CreateGameObject();
    auto transform = player->AddComponent<Transform>();
    auto rigidBody = player->AddComponent<RigidBody>();
    
    // 设置初始状态
    transform->SetPosition(glm::vec3(0.0f, 0.0f, 0.0f));
    rigidBody->SetMass(1.0f);
    
    // 游戏循环
    auto lastTime = std::chrono::high_resolution_clock::now();
    while (true) {
        auto currentTime = std::chrono::high_resolution_clock::now();
        float deltaTime = std::chrono::duration<float>(
            currentTime - lastTime
        ).count();
        lastTime = currentTime;
        
        // 更新场景
        scene.Update(deltaTime);
        
        // 模拟玩家输入
        rigidBody->ApplyForce(glm::vec3(0.0f, 9.8f, 0.0f));
        
        // ... 渲染和其他系统更新 ...
    }
    
    return 0;
}`
    },
    kotlin: {
        name: 'Kotlin (Android)',
        code: `package com.example.weatherapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.Response
import javax.inject.Inject

// 数据类定义
data class WeatherData(
    val location: String,
    val temperature: Double,
    val condition: String,
    val forecast: List<DayForecast>
)

data class DayForecast(
    val date: String,
    val temperature: Double,
    val condition: String
)

sealed class WeatherUiState {
    object Loading : WeatherUiState()
    data class Success(val data: WeatherData) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val locationTracker: LocationTracker,
    private val weatherMapper: WeatherMapper
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _locationPermissionGranted = MutableStateFlow(false)
    val locationPermissionGranted: StateFlow<Boolean> = _locationPermissionGranted.asStateFlow()

    init {
        viewModelScope.launch {
            locationTracker.locationFlow
                .filterNotNull()
                .collect { location ->
                    fetchWeatherData(location)
                }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _locationPermissionGranted.value = granted
        if (granted) {
            viewModelScope.launch {
                locationTracker.startLocationUpdates()
            }
        }
    }

    private suspend fun fetchWeatherData(location: Location) {
        _uiState.value = WeatherUiState.Loading

        try {
            weatherRepository.getWeatherData(location)
                .catch { e ->
                    _uiState.value = WeatherUiState.Error(e.message ?: "Unknown error")
                }
                .collect { response ->
                    when (response) {
                        is Response.Success -> {
                            val weatherData = weatherMapper.mapToWeatherData(response.data)
                            _uiState.value = WeatherUiState.Success(weatherData)
                        }
                        is Response.Error -> {
                            _uiState.value = WeatherUiState.Error(response.message)
                        }
                    }
                }
        } catch (e: Exception) {
            _uiState.value = WeatherUiState.Error(e.message ?: "Unknown error")
        }
    }

    fun refresh() {
        viewModelScope.launch {
            locationTracker.getCurrentLocation()?.let { location ->
                fetchWeatherData(location)
            }
        }
    }
}

@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val locationPermissionGranted by viewModel.locationPermissionGranted.collectAsState()

    LaunchedEffect(locationPermissionGranted) {
        if (!locationPermissionGranted) {
            requestLocationPermission()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weather App") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is WeatherUiState.Loading -> LoadingScreen()
                is WeatherUiState.Success -> WeatherContent(state.data)
                is WeatherUiState.Error -> ErrorScreen(state.message)
            }
        }
    }
}

@Composable
fun WeatherContent(data: WeatherData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        WeatherHeader(data)
        Spacer(modifier = Modifier.height(16.dp))
        WeatherDetails(data)
        Spacer(modifier = Modifier.height(16.dp))
        ForecastList(data.forecast)
    }
}

@Composable
fun WeatherHeader(
    data: WeatherData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = data.location,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "\${data.temperature}°C",
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = data.condition,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}`
    },
    swift: {
        name: 'Swift (iOS)',
        code: `import SwiftUI
import Combine
import CoreLocation

// MARK: - Models
struct WeatherInfo: Codable, Identifiable {
    let id: UUID
    let temperature: Double
    let condition: String
    let humidity: Int
    let windSpeed: Double
    let forecast: [DayForecast]
    
    var temperatureFormatted: String {
        return String(format: "%.1f°C", temperature)
    }
}

struct DayForecast: Codable, Identifiable {
    let id: UUID
    let date: Date
    let high: Double
    let low: Double
    let condition: String
}

// MARK: - View Models
@MainActor
class WeatherViewModel: ObservableObject {
    @Published private(set) var state: LoadingState = .idle
    @Published var currentLocation: CLLocation?
    @Published var searchQuery = ""
    
    private let weatherService: WeatherServiceProtocol
    private let locationManager: LocationManager
    private var cancellables = Set<AnyCancellable>()
    
    enum LoadingState {
        case idle
        case loading
        case loaded(WeatherInfo)
        case error(Error)
    }
    
    init(weatherService: WeatherServiceProtocol = WeatherService(),
         locationManager: LocationManager = .shared) {
        self.weatherService = weatherService
        self.locationManager = locationManager
        
        setupLocationUpdates()
        setupSearchDebounce()
    }
    
    private func setupLocationUpdates() {
        locationManager.$location
            .compactMap { $0 }
            .removeDuplicates()
            .sink { [weak self] location in
                self?.currentLocation = location
                Task {
                    await self?.fetchWeather(for: location)
                }
            }
            .store(in: &cancellables)
    }
    
    private func setupSearchDebounce() {
        $searchQuery
            .debounce(for: .milliseconds(500), scheduler: DispatchQueue.main)
            .removeDuplicates()
            .sink { [weak self] query in
                guard !query.isEmpty else { return }
                Task {
                    await self?.searchLocation(query)
                }
            }
            .store(in: &cancellables)
    }
    
    @MainActor
    func fetchWeather(for location: CLLocation) async {
        state = .loading
        
        do {
            let weather = try await weatherService.fetchWeather(
                latitude: location.coordinate.latitude,
                longitude: location.coordinate.longitude
            )
            state = .loaded(weather)
        } catch {
            state = .error(error)
        }
    }
}

// MARK: - Views
struct WeatherView: View {
    @StateObject private var viewModel = WeatherViewModel()
    @Environment(\.colorScheme) var colorScheme
    
    var body: some View {
        NavigationStack {
            ZStack {
                backgroundGradient
                
                ScrollView {
                    VStack(spacing: 20) {
                        searchBar
                        
                        switch viewModel.state {
                        case .idle:
                            EmptyStateView()
                        case .loading:
                            LoadingView()
                        case .loaded(let weather):
                            WeatherContentView(weather: weather)
                        case .error(let error):
                            ErrorView(error: error)
                        }
                    }
                    .padding()
                }
            }
            .navigationTitle("Weather")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: requestLocation) {
                        Image(systemName: "location.circle.fill")
                    }
                }
            }
        }
    }
    
    private var backgroundGradient: some View {
        LinearGradient(
            gradient: Gradient(colors: [
                Color(.systemBlue).opacity(0.6),
                Color(.systemTeal).opacity(0.4)
            ]),
            startPoint: .top,
            endPoint: .bottom
        )
        .ignoresSafeArea()
    }
    
    private var searchBar: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.gray)
            
            TextField("Search location...", text: $viewModel.searchQuery)
                .textFieldStyle(RoundedBorderTextFieldStyle())
                .autocapitalization(.words)
        }
        .padding(.horizontal)
    }
    
    private func requestLocation() {
        viewModel.locationManager.requestLocation()
    }
}

struct WeatherContentView: View {
    let weather: WeatherInfo
    
    var body: some View {
        VStack(spacing: 24) {
            CurrentWeatherCard(weather: weather)
            
            ForecastList(forecast: weather.forecast)
            
            WeatherDetailsGrid(weather: weather)
        }
        .transition(.opacity.combined(with: .move(edge: .bottom)))
    }
}

// MARK: - Preview
struct WeatherView_Previews: PreviewProvider {
    static var previews: some View {
        WeatherView()
    }
}`
    },
    ruby: {
        name: 'Ruby (Rails)',
        code: `# app/models/order.rb
class Order < ApplicationRecord
  include AASM
  include Searchable
  
  belongs_to :user
  has_many :order_items, dependent: :destroy
  has_many :products, through: :order_items
  has_one :payment, dependent: :destroy
  has_one :shipping_address, dependent: :destroy
  
  validates :number, presence: true, uniqueness: true
  validates :total_amount, presence: true, numericality: { greater_than_or_equal_to: 0 }
  
  before_validation :generate_order_number, on: :create
  after_create :notify_admin
  
  scope :recent, -> { where('created_at > ?', 30.days.ago) }
  scope :paid, -> { where(aasm_state: 'paid') }
  scope :by_status, ->(status) { where(aasm_state: status) if status.present? }
  
  aasm do
    state :pending, initial: true
    state :processing, :paid, :shipped, :delivered, :cancelled, :refunded
    
    event :process do
      transitions from: :pending, to: :processing
      after do
        OrderMailer.processing_notification(self).deliver_later
        notify_slack
      end
    end
    
    event :mark_paid do
      transitions from: [:pending, :processing], to: :paid
      after do
        update_inventory
        create_invoice
      end
    end
    
    event :ship do
      transitions from: :paid, to: :shipped
      after do
        OrderMailer.shipping_notification(self).deliver_later
        update_shipping_status
      end
    end
    
    event :deliver do
      transitions from: :shipped, to: :delivered
      after do
        OrderMailer.delivery_confirmation(self).deliver_later
        update_loyalty_points
      end
    end
    
    event :cancel do
      transitions from: [:pending, :processing], to: :cancelled
      after do
        restock_items
        refund_payment if paid?
      end
    end
    
    event :refund do
      transitions from: [:paid, :delivered], to: :refunded
      after do
        process_refund
        notify_customer
      end
    end
  end
  
  def total_items
    order_items.sum(:quantity)
  end
  
  def available_shipping_methods
    ShippingCalculator.new(self).available_methods
  end
  
  def apply_coupon(code)
    coupon = Coupon.find_by(code: code)
    return false unless coupon&.valid_for?(self)
    
    transaction do
      self.discount_amount = coupon.calculate_discount(total_amount)
      self.coupon = coupon
      save!
    end
  rescue ActiveRecord::RecordInvalid
    false
  end
  
  private
  
  def generate_order_number
    return if number.present?
    
    loop do
      self.number = SecureRandom.uuid
      break unless Order.exists?(number: number)
    end
  end
  
  def notify_admin
    AdminNotifier.new_order(self).deliver_later
  end
  
  def update_inventory
    order_items.each do |item|
      item.product.decrement_stock!(item.quantity)
    end
  end
  
  def create_invoice
    Invoice.create!(
      order: self,
      amount: total_amount,
      tax_amount: calculate_tax,
      billing_address: user.billing_address
    )
  end
  
  def update_loyalty_points
    points = LoyaltyPointsCalculator.new(self).calculate
    user.increment!(:loyalty_points, points)
  end
  
  def notify_slack
    SlackNotifier.notify(
      channel: '#orders',
      text: "New order ##{number} (#{total_amount_formatted}) from #{user.email}"
    )
  end
  
  def calculate_tax
    TaxCalculator.new(self).calculate
  end
  
  def restock_items
    order_items.each do |item|
      item.product.increment_stock!(item.quantity)
    end
  end
  
  def process_refund
    payment.refund!
    update_inventory_after_refund
  end
  
  def notify_customer
    OrderMailer.refund_notification(self).deliver_later
  end
end

# app/controllers/api/v1/orders_controller.rb
module Api
  module V1
    class OrdersController < ApiController
      before_action :authenticate_user!
      before_action :set_order, only: [:show, :update, :cancel, :refund]
      
      def index
        @orders = current_user.orders
          .includes(:order_items, :products)
          .by_status(params[:status])
          .page(params[:page])
          .per(params[:per_page])
        
        render json: OrderSerializer.new(@orders).serializable_hash
      end
      
      def show
        render json: OrderSerializer.new(@order, include: [:order_items, :payment]).serializable_hash
      end
      
      def create
        @order = OrderService.new(current_user, order_params).create_order
        
        if @order.persisted?
          render json: OrderSerializer.new(@order).serializable_hash, status: :created
        else
          render_error(@order.errors)
        end
      end
      
      def cancel
        if @order.cancel
          render json: { message: 'Order cancelled successfully' }
        else
          render_error('Unable to cancel order')
        end
      end
      
      private
      
      def set_order
        @order = current_user.orders.find(params[:id])
      end
      
      def order_params
        params.require(:order).permit(
          :coupon_code,
          :shipping_method,
          order_items_attributes: [:product_id, :quantity],
          shipping_address_attributes: [:street, :city, :state, :zip]
        )
      end
    end
  end
end`
    },
    php: {
        name: 'PHP (Laravel)',
        code: `<?php

namespace App\\Http\\Controllers;

use App\\Models\\Post;
use App\\Services\\ImageService;
use App\\Http\\Requests\\PostRequest;
use App\\Events\\PostPublished;
use Illuminate\\Support\\Facades\\Cache;
use Illuminate\\Support\\Facades\\DB;
use Illuminate\\Support\\Facades\\Log;

class PostController extends Controller
{
    private ImageService $imageService;
    
    public function __construct(ImageService $imageService)
    {
        $this->imageService = $imageService;
        $this->middleware('auth')->except(['index', 'show']);
        $this->middleware('can:manage,post')->only(['edit', 'update', 'destroy']);
    }
    
    public function index()
    {
        $posts = Cache::tags(['posts'])->remember('posts.page.' . request('page', 1), 3600, function () {
            return Post::with(['author', 'categories', 'tags'])
                ->withCount(['comments', 'likes'])
                ->published()
                ->latest()
                ->paginate(15);
        });
        
        return view('posts.index', compact('posts'));
    }
    
    public function show(Post $post)
    {
        $post->load(['author', 'comments.author', 'categories', 'tags']);
        $relatedPosts = $post->getRelatedPosts();
        
        // 增加浏览次数
        $post->increment('views');
        
        return view('posts.show', compact('post', 'relatedPosts'));
    }
    
    public function store(PostRequest $request)
    {
        try {
            DB::beginTransaction();
            
            $post = new Post($request->validated());
            $post->author_id = auth()->id();
            
            if ($request->hasFile('cover_image')) {
                $post->cover_image = $this->imageService->upload(
                    $request->file('cover_image'),
                    'posts/covers'
                );
            }
            
            $post->save();
            
            // 处理标签和分类
            $post->categories()->sync($request->input('categories', []));
            $post->tags()->sync($this->processTags($request->input('tags', [])));
            
            // 处理内容中的图片
            $post->content = $this->processContentImages($post->content);
            $post->save();
            
            DB::commit();
            
            if ($post->status === 'published') {
                event(new PostPublished($post));
            }
            
            Cache::tags(['posts'])->flush();
            
            return response()->json([
                'message' => 'Post created successfully',
                'post' => $post->load(['author', 'categories', 'tags'])
            ], 201);
            
        } catch (\Exception $e) {
            DB::rollBack();
            Log::error('Post creation failed', [
                'error' => $e->getMessage(),
                'user_id' => auth()->id(),
                'request_data' => $request->validated()
            ]);
            
            return response()->json([
                'message' => 'Failed to create post',
                'error' => config('app.debug') ? $e->getMessage() : 'Internal server error'
            ], 500);
        }
    }
    
    public function update(PostRequest $request, Post $post)
    {
        try {
            DB::beginTransaction();
            
            $oldStatus = $post->status;
            $post->fill($request->validated());
            
            if ($request->hasFile('cover_image')) {
                $this->imageService->delete($post->cover_image);
                $post->cover_image = $this->imageService->upload(
                    $request->file('cover_image'),
                    'posts/covers'
                );
            }
            
            // 处理内容中的图片
            $post->content = $this->processContentImages($post->content);
            
            $post->save();
            
            // 处理标签和分类
            $post->categories()->sync($request->input('categories', []));
            $post->tags()->sync($this->processTags($request->input('tags', [])));
            
            DB::commit();
            
            // 如果文章状态从草稿改为发布，触发事件
            if ($oldStatus !== 'published' && $post->status === 'published') {
                event(new PostPublished($post));
            }
            
            Cache::tags(['posts'])->flush();
            
            return response()->json([
                'message' => 'Post updated successfully',
                'post' => $post->load(['author', 'categories', 'tags'])
            ]);
            
        } catch (\Exception $e) {
            DB::rollBack();
            Log::error('Post update failed', [
                'error' => $e->getMessage(),
                'post_id' => $post->id,
                'user_id' => auth()->id()
            ]);
            
            return response()->json([
                'message' => 'Failed to update post',
                'error' => config('app.debug') ? $e->getMessage() : 'Internal server error'
            ], 500);
        }
    }
    
    private function processTags(array $tags): array
    {
        return collect($tags)
            ->map(function ($tag) {
                if (is_numeric($tag)) {
                    return $tag;
                }
                
                return DB::table('tags')->insertGetId([
                    'name' => $tag,
                    'slug' => str_slug($tag),
                    'created_at' => now(),
                    'updated_at' => now()
                ]);
            })
            ->toArray();
    }
    
    private function processContentImages(string $content): string
    {
        return preg_replace_callback('/src="data:image\\/(.*?);base64,([^"]*)"/', function ($matches) {
            $image = base64_decode($matches[2]);
            $extension = $matches[1];
            
            $path = $this->imageService->uploadBase64(
                $image,
                $extension,
                'posts/content'
            );
            
            return 'src="' . asset($path) . '"';
        }, $content);
    }
}`
    },
    html: {
        name: 'HTML/CSS',
        code: `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Modern Portfolio</title>
    <style>
        /* 现代化的CSS变量系统 */
        :root {
            --primary-color: #2563eb;
            --secondary-color: #3b82f6;
            --accent-color: #60a5fa;
            --text-primary: #1f2937;
            --text-secondary: #4b5563;
            --background: #ffffff;
            --background-alt: #f3f4f6;
            
            --spacing-xs: 0.25rem;
            --spacing-sm: 0.5rem;
            --spacing-md: 1rem;
            --spacing-lg: 1.5rem;
            --spacing-xl: 2rem;
            
            --border-radius: 0.5rem;
            --transition: all 0.3s ease;
        }

        /* 深色模式支持 */
        @media (prefers-color-scheme: dark) {
            :root {
                --primary-color: #3b82f6;
                --secondary-color: #60a5fa;
                --accent-color: #93c5fd;
                --text-primary: #f9fafb;
                --text-secondary: #e5e7eb;
                --background: #111827;
                --background-alt: #1f2937;
            }
        }

        /* 基础样式重置和设置 */
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: system-ui, -apple-system, sans-serif;
            line-height: 1.5;
            color: var(--text-primary);
            background: var(--background);
        }

        /* 响应式容器 */
        .container {
            width: min(90%, 1200px);
            margin: 0 auto;
            padding: var(--spacing-md);
        }

        /* 现代化的网格系统 */
        .grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: var(--spacing-lg);
        }

        /* 卡片组件 */
        .card {
            background: var(--background-alt);
            border-radius: var(--border-radius);
            padding: var(--spacing-lg);
            transition: var(--transition);
            
            &:hover {
                transform: translateY(-4px);
                box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
            }
        }

        /* 现代化的导航栏 */
        .navbar {
            position: sticky;
            top: 0;
            background: var(--background);
            backdrop-filter: blur(10px);
            border-bottom: 1px solid rgba(0, 0, 0, 0.1);
            z-index: 1000;
        }

        .nav-content {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: var(--spacing-md) 0;
        }

        /* 响应式导航菜单 */
        .nav-menu {
            display: flex;
            gap: var(--spacing-md);
            
            @media (max-width: 768px) {
                position: fixed;
                top: 0;
                right: 0;
                bottom: 0;
                background: var(--background);
                padding: var(--spacing-xl);
                transform: translateX(100%);
                transition: var(--transition);
                
                &.active {
                    transform: translateX(0);
                }
                
                flex-direction: column;
                justify-content: center;
                min-width: 250px;
            }
        }

        /* 现代化的按钮样式 */
        .button {
            display: inline-flex;
            align-items: center;
            gap: var(--spacing-xs);
            padding: var(--spacing-sm) var(--spacing-md);
            background: var(--primary-color);
            color: white;
            border: none;
            border-radius: var(--border-radius);
            cursor: pointer;
            transition: var(--transition);
            
            &:hover {
                background: var(--secondary-color);
            }
            
            &.secondary {
                background: transparent;
                border: 1px solid var(--primary-color);
                color: var(--primary-color);
                
                &:hover {
                    background: var(--primary-color);
                    color: white;
                }
            }
        }

        /* 动画效果 */
        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(20px); }
            to { opacity: 1; transform: translateY(0); }
        }

        .animate-in {
            animation: fadeIn 0.6s ease forwards;
            opacity: 0;
        }

        /* 响应式图片 */
        .responsive-image {
            width: 100%;
            height: auto;
            border-radius: var(--border-radius);
            aspect-ratio: 16/9;
            object-fit: cover;
        }

        /* 现代化的表单样式 */
        .form-group {
            margin-bottom: var(--spacing-md);
        }

        .input {
            width: 100%;
            padding: var(--spacing-sm);
            border: 1px solid var(--text-secondary);
            border-radius: var(--border-radius);
            background: var(--background);
            color: var(--text-primary);
            transition: var(--transition);
            
            &:focus {
                outline: none;
                border-color: var(--primary-color);
                box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
            }
        }

        /* 加载动画 */
        .loading {
            display: inline-block;
            width: 24px;
            height: 24px;
            border: 2px solid var(--background);
            border-radius: 50%;
            border-top-color: var(--primary-color);
            animation: spin 0.6s linear infinite;
        }

        @keyframes spin {
            to { transform: rotate(360deg); }
        }
    </style>
</head>
<body>
    <nav class="navbar">
        <div class="container">
            <div class="nav-content">
                <a href="#" class="logo">Portfolio</a>
                <button class="nav-toggle" aria-label="Toggle navigation">
                    <span></span>
                </button>
                <div class="nav-menu">
                    <a href="#work">Work</a>
                    <a href="#about">About</a>
                    <a href="#contact">Contact</a>
                    <button class="button">Get in touch</button>
                </div>
            </div>
        </div>
    </nav>

    <main>
        <section class="hero">
            <div class="container">
                <h1 class="animate-in">Creative Developer & Designer</h1>
                <p class="animate-in">Building beautiful and functional web experiences</p>
                <div class="button-group">
                    <button class="button">View Work</button>
                    <button class="button secondary">Contact Me</button>
                </div>
            </div>
        </section>

        <section id="work" class="work">
            <div class="container">
                <h2>Featured Work</h2>
                <div class="grid">
                    <article class="card">
                        <img src="project1.jpg" alt="Project 1" class="responsive-image">
                        <h3>Project Title</h3>
                        <p>Project description goes here</p>
                        <a href="#" class="button secondary">View Project</a>
                    </article>
                    <!-- More project cards -->
                </div>
            </div>
        </section>

        <section id="contact" class="contact">
            <div class="container">
                <h2>Get in Touch</h2>
                <form class="contact-form">
                    <div class="form-group">
                        <label for="name">Name</label>
                        <input type="text" id="name" class="input" required>
                    </div>
                    <div class="form-group">
                        <label for="email">Email</label>
                        <input type="email" id="email" class="input" required>
                    </div>
                    <div class="form-group">
                        <label for="message">Message</label>
                        <textarea id="message" class="input" rows="5" required></textarea>
                    </div>
                    <button type="submit" class="button">
                        <span>Send Message</span>
                        <div class="loading" hidden></div>
                    </button>
                </form>
            </div>
        </section>
    </main>

    <footer>
        <div class="container">
            <p>&copy; 2024 Portfolio. All rights reserved.</p>
        </div>
    </footer>
</body>
</html>`
    }
    // ... 继续添加其他语言的示例 ...
}; 