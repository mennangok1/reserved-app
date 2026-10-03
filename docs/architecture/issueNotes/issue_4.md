### Issue #4 Create Entity classes 

## Primary utilized annotations:
- @Entity
- @Getter
- @Setter
- @GeneratedValue
- @Id
- @Column
- @JoinColumn
- @ManyToOne
- @OneToOne

## Modified ER Diagram decision:
* Added ItemType entity to hold MenuItem's itemType field
* Deleted restaurantType field from Restaurant for the MVP

Also added TestcontainersConfiguration.java for test containers
