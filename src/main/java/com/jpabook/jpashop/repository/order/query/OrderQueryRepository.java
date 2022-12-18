package com.jpabook.jpashop.repository.order.query;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderQueryRepository {

    private final EntityManager em;


    public List<OrderQueryDto> findOrderQueryDtos() {
        List<OrderQueryDto> result = findOrders();

        result.forEach(orderQueryDto -> {
            List<OrderItemQueryDto> orderItems = findOrderItems(orderQueryDto.getOrderId());
            orderQueryDto.setOrderItems(orderItems);
        });

        return result;
    }

    private List<OrderItemQueryDto> findOrderItems(Long orderId) {
        return em.createQuery(
                "select new com.jpabook.jpashop.repository.order.query.OrderItemQueryDto(oi.order.id, oi.item.name, oi.orderPrice, oi.count)"
                    + "from OrderItem oi"
                    + " join oi.item i"
                    + " where oi.order.id = :orderId", OrderItemQueryDto.class
            )
            .setParameter("orderId", orderId)
            .getResultList();
    }

    private List<OrderQueryDto> findOrders() {
        return em.createQuery(
            "select new com.jpabook.jpashop.repository.order.query.OrderQueryDto(o.id, m.name, o.orderDate, o.status, d.address)"
                + " from Order o"
                + " join o.member m"
                + " join o.delivery d", OrderQueryDto.class
        ).getResultList();
    }

    public List<OrderQueryDto> findAllByDtoOptimization() {
        List<OrderQueryDto> orders = findOrders();

        Map<Long, List<OrderItemQueryDto>> orderItemMap = findOrderItemMap(toOrderId(orders));

        // 위 map에서 orderId 기준으로 다시 데이터를 뽑아서 dto에 넣어준다.
        orders.forEach(orderQueryDto -> {
            orderQueryDto.setOrderItems(orderItemMap.get(orderQueryDto.getOrderId()));
        });

        return orders;
    }

    private Map<Long, List<OrderItemQueryDto>> findOrderItemMap(List<Long> orderIds) {
        List<OrderItemQueryDto> orderItems = em.createQuery(
                "select new com.jpabook.jpashop.repository.order.query.OrderItemQueryDto(oi.order.id, oi.item.name, oi.orderPrice, oi.count)"
                    + "from OrderItem oi"
                    + " join oi.item i"
                    + " where oi.order.id in :orderIds"
                , OrderItemQueryDto.class)
            .setParameter("orderIds", orderIds)
            .getResultList();

        // 위에 결과 list를 orderId 기준으로 map으로 변환
        Map<Long, List<OrderItemQueryDto>> orderItemMap = orderItems.stream()
            .collect(Collectors.groupingBy(orderItemQueryDto -> orderItemQueryDto.getOrderId()));
        return orderItemMap;
    }

    private static List<Long> toOrderId(List<OrderQueryDto> orders) {
        List<Long> orderIds = orders.stream()
            .map(o -> o.getOrderId())
            .collect(Collectors.toList());
        return orderIds;
    }

    public List<OrderFlatDto> findAllByDtoFlat() {
        return em.createQuery(
            "select new com.jpabook.jpashop.repository.order.query.OrderFlatDto(o.id, m.name, o.orderDate, o.status, d.address, i.name, oi.orderPrice, oi.count)"
                + " from Order o"
                + " join o.member m"
                + " join o.delivery d"
                + " join o.orderItems oi"
                + " join oi.item i", OrderFlatDto.class
        ).getResultList();
    }
}
