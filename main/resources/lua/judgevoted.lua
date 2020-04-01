local arrys=redis.call('hgetall',KEYS[1])
for k,v in pairs(arrys) do
    if(string.find(v,ARGV[1])~= nil)
        then
            return arrys[k-1]
    end
end
return 0
