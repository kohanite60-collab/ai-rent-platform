# ai-rent-platform

这是一个基于springboot编写的ai算力租赁项目，也是web考核项目

技术选型：springboot3.2.12+redis+mybatis-plus+mysql+springai

8.22

使用了mybatis-plus重构了这个项目的查询方式，感觉比手写sql快多了



8.23

用redis存键值实现了每日签到的接口，但是此功能存在bug，操作不满足原子性，并发情况下会产生多加算力的情况，而且如果添加完key之后正好服务器坏了，服务器修好后今日就不能签到了hh
