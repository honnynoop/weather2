package com.mds.weather.repository;

import com.mds.weather.domain.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * raw.orders 테이블 Repository
 *
 * <p>[수정] LazyInitializationException 방지:
 * findTop10WithProductByCustomerId — JOIN FETCH 로 Product 함께 로드
 *
 * <p>기존 findTop10ByCustomer_CustomerIdOrderByOrderDateDesc 는
 * Product 를 LAZY 로딩하므로 @Transactional 없이 호출하면
 * 프록시 초기화 실패. JOIN FETCH 쿼리로 대체.
 */
public interface SalesOrderRepository
        extends JpaRepository<SalesOrder, Long> {

    /**
     * 고객 최근 주문 10건 — Product JOIN FETCH (N+1 방지)
     *
     * <p>WeatherRecommendationService 에서 사용.
     * o.getProduct().getCategory() / getName() 을 트랜잭션 안에서
     * 안전하게 접근 가능하도록 Product 를 함께 로드.
     */
    @Query("""
            SELECT o FROM SalesOrder o
            JOIN FETCH o.product
            WHERE o.customer.customerId = :customerId
            ORDER BY o.orderDate DESC
            """)
    List<SalesOrder> findTop10WithProductByCustomerId(
            @Param("customerId") Long customerId,
            org.springframework.data.domain.Pageable pageable);

    /**
     * 편의 메서드 — pageable 없이 상위 10건만 조회
     */
    default List<SalesOrder> findTop10WithProductByCustomerId(Long customerId) {
        return findTop10WithProductByCustomerId(
                customerId,
                org.springframework.data.domain.PageRequest.of(0, 10));
    }

    /** 날짜 범위 완료 주문 — Customer, Product 함께 로드 */
    @Query("""
            SELECT o FROM SalesOrder o
            JOIN FETCH o.product
            JOIN FETCH o.customer
            WHERE o.orderDate BETWEEN :from AND :to
              AND o.status = 'completed'
            """)
    List<SalesOrder> findCompletedBetween(@Param("from") LocalDate from,
                                          @Param("to")   LocalDate to);

    /** 카테고리별 매출 Top N (네이티브 SQL — Product 전체 로드 불필요) */
    @Query(value = """
            SELECT p.category, COUNT(*) AS cnt,
                   SUM(o.quantity * o.unit_price * (1 - o.discount_rate)) AS revenue
            FROM raw.orders o
            JOIN raw.products p ON o.product_id = p.product_id
            WHERE o.order_date = :date AND o.status = 'completed'
            GROUP BY p.category
            ORDER BY revenue DESC
            LIMIT :topN
            """, nativeQuery = true)
    List<Object[]> findTopCategoriesByDate(@Param("date") LocalDate date,
                                            @Param("topN") int topN);
}
