package io.github.jhipster.application.web.rest;

import io.github.jhipster.application.CrmApp;

import io.github.jhipster.application.domain.OrderItem;
import io.github.jhipster.application.domain.Product;
import io.github.jhipster.application.domain.ProductOrder;
import io.github.jhipster.application.repository.OrderItemRepository;
import io.github.jhipster.application.repository.search.OrderItemSearchRepository;
import io.github.jhipster.application.service.OrderItemService;
import io.github.jhipster.application.web.rest.errors.ExceptionTranslator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;


import static io.github.jhipster.application.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.github.jhipster.application.domain.enumeration.OrderItemStatus;
/**
 * Test class for the OrderItemResource REST controller.
 *
 * @see OrderItemResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = CrmApp.class)
public class OrderItemResourceIntTest {

    private static final Integer DEFAULT_QUANTITY = 0;
    private static final Integer UPDATED_QUANTITY = 1;

    private static final BigDecimal DEFAULT_TOTAL_PRICE = new BigDecimal(0);
    private static final BigDecimal UPDATED_TOTAL_PRICE = new BigDecimal(1);

    private static final OrderItemStatus DEFAULT_STATUS = OrderItemStatus.AVAILABLE;
    private static final OrderItemStatus UPDATED_STATUS = OrderItemStatus.OUT_OF_STOCK;

    @Autowired
    private OrderItemRepository orderItemRepository;
    
    @Autowired
    private OrderItemService orderItemService;

    /**
     * This repository is mocked in the io.github.jhipster.application.repository.search test package.
     *
     * @see io.github.jhipster.application.repository.search.OrderItemSearchRepositoryMockConfiguration
     */
    @Autowired
    private OrderItemSearchRepository mockOrderItemSearchRepository;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    private MockMvc restOrderItemMockMvc;

    private OrderItem orderItem;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final OrderItemResource orderItemResource = new OrderItemResource(orderItemService);
        this.restOrderItemMockMvc = MockMvcBuilders.standaloneSetup(orderItemResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static OrderItem createEntity(EntityManager em) {
        OrderItem orderItem = new OrderItem()
            .quantity(DEFAULT_QUANTITY)
            .totalPrice(DEFAULT_TOTAL_PRICE)
            .status(DEFAULT_STATUS);
        // Add required entity
        Product product = ProductResourceIntTest.createEntity(em);
        em.persist(product);
        em.flush();
        orderItem.setProduct(product);
        // Add required entity
        ProductOrder productOrder = ProductOrderResourceIntTest.createEntity(em);
        em.persist(productOrder);
        em.flush();
        orderItem.setOrder(productOrder);
        return orderItem;
    }

    @Before
    public void initTest() {
        orderItem = createEntity(em);
    }

    @Test
    @Transactional
    public void createOrderItem() throws Exception {
        int databaseSizeBeforeCreate = orderItemRepository.findAll().size();

        // Create the OrderItem
        restOrderItemMockMvc.perform(post("/api/order-items")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(orderItem)))
            .andExpect(status().isCreated());

        // Validate the OrderItem in the database
        List<OrderItem> orderItemList = orderItemRepository.findAll();
        assertThat(orderItemList).hasSize(databaseSizeBeforeCreate + 1);
        OrderItem testOrderItem = orderItemList.get(orderItemList.size() - 1);
        assertThat(testOrderItem.getQuantity()).isEqualTo(DEFAULT_QUANTITY);
        assertThat(testOrderItem.getTotalPrice()).isEqualTo(DEFAULT_TOTAL_PRICE);
        assertThat(testOrderItem.getStatus()).isEqualTo(DEFAULT_STATUS);

        // Validate the OrderItem in Elasticsearch
        verify(mockOrderItemSearchRepository, times(1)).save(testOrderItem);
    }

    @Test
    @Transactional
    public void createOrderItemWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = orderItemRepository.findAll().size();

        // Create the OrderItem with an existing ID
        orderItem.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restOrderItemMockMvc.perform(post("/api/order-items")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(orderItem)))
            .andExpect(status().isBadRequest());

        // Validate the OrderItem in the database
        List<OrderItem> orderItemList = orderItemRepository.findAll();
        assertThat(orderItemList).hasSize(databaseSizeBeforeCreate);

        // Validate the OrderItem in Elasticsearch
        verify(mockOrderItemSearchRepository, times(0)).save(orderItem);
    }

    @Test
    @Transactional
    public void checkQuantityIsRequired() throws Exception {
        int databaseSizeBeforeTest = orderItemRepository.findAll().size();
        // set the field null
        orderItem.setQuantity(null);

        // Create the OrderItem, which fails.

        restOrderItemMockMvc.perform(post("/api/order-items")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(orderItem)))
            .andExpect(status().isBadRequest());

        List<OrderItem> orderItemList = orderItemRepository.findAll();
        assertThat(orderItemList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkTotalPriceIsRequired() throws Exception {
        int databaseSizeBeforeTest = orderItemRepository.findAll().size();
        // set the field null
        orderItem.setTotalPrice(null);

        // Create the OrderItem, which fails.

        restOrderItemMockMvc.perform(post("/api/order-items")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(orderItem)))
            .andExpect(status().isBadRequest());

        List<OrderItem> orderItemList = orderItemRepository.findAll();
        assertThat(orderItemList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkStatusIsRequired() throws Exception {
        int databaseSizeBeforeTest = orderItemRepository.findAll().size();
        // set the field null
        orderItem.setStatus(null);

        // Create the OrderItem, which fails.

        restOrderItemMockMvc.perform(post("/api/order-items")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(orderItem)))
            .andExpect(status().isBadRequest());

        List<OrderItem> orderItemList = orderItemRepository.findAll();
        assertThat(orderItemList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllOrderItems() throws Exception {
        // Initialize the database
        orderItemRepository.saveAndFlush(orderItem);

        // Get all the orderItemList
        restOrderItemMockMvc.perform(get("/api/order-items?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(orderItem.getId().intValue())))
            .andExpect(jsonPath("$.[*].quantity").value(hasItem(DEFAULT_QUANTITY)))
            .andExpect(jsonPath("$.[*].totalPrice").value(hasItem(DEFAULT_TOTAL_PRICE.intValue())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())));
    }
    
    @Test
    @Transactional
    public void getOrderItem() throws Exception {
        // Initialize the database
        orderItemRepository.saveAndFlush(orderItem);

        // Get the orderItem
        restOrderItemMockMvc.perform(get("/api/order-items/{id}", orderItem.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(orderItem.getId().intValue()))
            .andExpect(jsonPath("$.quantity").value(DEFAULT_QUANTITY))
            .andExpect(jsonPath("$.totalPrice").value(DEFAULT_TOTAL_PRICE.intValue()))
            .andExpect(jsonPath("$.status").value(DEFAULT_STATUS.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingOrderItem() throws Exception {
        // Get the orderItem
        restOrderItemMockMvc.perform(get("/api/order-items/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateOrderItem() throws Exception {
        // Initialize the database
        orderItemService.save(orderItem);
        // As the test used the service layer, reset the Elasticsearch mock repository
        reset(mockOrderItemSearchRepository);

        int databaseSizeBeforeUpdate = orderItemRepository.findAll().size();

        // Update the orderItem
        OrderItem updatedOrderItem = orderItemRepository.findById(orderItem.getId()).get();
        // Disconnect from session so that the updates on updatedOrderItem are not directly saved in db
        em.detach(updatedOrderItem);
        updatedOrderItem
            .quantity(UPDATED_QUANTITY)
            .totalPrice(UPDATED_TOTAL_PRICE)
            .status(UPDATED_STATUS);

        restOrderItemMockMvc.perform(put("/api/order-items")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedOrderItem)))
            .andExpect(status().isOk());

        // Validate the OrderItem in the database
        List<OrderItem> orderItemList = orderItemRepository.findAll();
        assertThat(orderItemList).hasSize(databaseSizeBeforeUpdate);
        OrderItem testOrderItem = orderItemList.get(orderItemList.size() - 1);
        assertThat(testOrderItem.getQuantity()).isEqualTo(UPDATED_QUANTITY);
        assertThat(testOrderItem.getTotalPrice()).isEqualTo(UPDATED_TOTAL_PRICE);
        assertThat(testOrderItem.getStatus()).isEqualTo(UPDATED_STATUS);

        // Validate the OrderItem in Elasticsearch
        verify(mockOrderItemSearchRepository, times(1)).save(testOrderItem);
    }

    @Test
    @Transactional
    public void updateNonExistingOrderItem() throws Exception {
        int databaseSizeBeforeUpdate = orderItemRepository.findAll().size();

        // Create the OrderItem

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restOrderItemMockMvc.perform(put("/api/order-items")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(orderItem)))
            .andExpect(status().isBadRequest());

        // Validate the OrderItem in the database
        List<OrderItem> orderItemList = orderItemRepository.findAll();
        assertThat(orderItemList).hasSize(databaseSizeBeforeUpdate);

        // Validate the OrderItem in Elasticsearch
        verify(mockOrderItemSearchRepository, times(0)).save(orderItem);
    }

    @Test
    @Transactional
    public void deleteOrderItem() throws Exception {
        // Initialize the database
        orderItemService.save(orderItem);

        int databaseSizeBeforeDelete = orderItemRepository.findAll().size();

        // Get the orderItem
        restOrderItemMockMvc.perform(delete("/api/order-items/{id}", orderItem.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<OrderItem> orderItemList = orderItemRepository.findAll();
        assertThat(orderItemList).hasSize(databaseSizeBeforeDelete - 1);

        // Validate the OrderItem in Elasticsearch
        verify(mockOrderItemSearchRepository, times(1)).deleteById(orderItem.getId());
    }

    @Test
    @Transactional
    public void searchOrderItem() throws Exception {
        // Initialize the database
        orderItemService.save(orderItem);
        when(mockOrderItemSearchRepository.search(queryStringQuery("id:" + orderItem.getId()), PageRequest.of(0, 20)))
            .thenReturn(new PageImpl<>(Collections.singletonList(orderItem), PageRequest.of(0, 1), 1));
        // Search the orderItem
        restOrderItemMockMvc.perform(get("/api/_search/order-items?query=id:" + orderItem.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(orderItem.getId().intValue())))
            .andExpect(jsonPath("$.[*].quantity").value(hasItem(DEFAULT_QUANTITY)))
            .andExpect(jsonPath("$.[*].totalPrice").value(hasItem(DEFAULT_TOTAL_PRICE.intValue())))
            .andExpect(jsonPath("$.[*].status").value(hasItem(DEFAULT_STATUS.toString())));
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(OrderItem.class);
        OrderItem orderItem1 = new OrderItem();
        orderItem1.setId(1L);
        OrderItem orderItem2 = new OrderItem();
        orderItem2.setId(orderItem1.getId());
        assertThat(orderItem1).isEqualTo(orderItem2);
        orderItem2.setId(2L);
        assertThat(orderItem1).isNotEqualTo(orderItem2);
        orderItem1.setId(null);
        assertThat(orderItem1).isNotEqualTo(orderItem2);
    }
}
