**关于本机启动redis的记录**



hostname -I 在虚拟机终端查看虚拟机ip，查到后进行相应配置



**在finalshell里面操作：**

locate redis.conf

找到位置后   sudo /usr/local/src/redis-6.2.7/src/redis-server /usr/local/src/redis-6.2.7/redis.conf

**从配置启动redis**



**防火墙配置：**

 sudo systemctl stop firewalld		暂时关闭防火墙，重启后恢复
 sudo systemctl disable firewalld		永久关闭

sudo systemctl status firewalld		查看防火墙状态



rabbitmq：sudo docker start rabbitmq