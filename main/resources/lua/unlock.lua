local key=KEYS[1]
local arg=ARGV[1]
local value=redis.call('get',key)
if(value==arg)
then
    return redis.call('del',key)
end
return 0
